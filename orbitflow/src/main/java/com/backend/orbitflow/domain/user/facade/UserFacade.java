package com.backend.orbitflow.domain.user.facade;

import com.backend.orbitflow.domain.auth.service.AuthService;
import com.backend.orbitflow.domain.auth.service.TokenService;
import com.backend.orbitflow.domain.avatar.service.AvatarService;
import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.dto.response.UserProfileResponse;
import com.backend.orbitflow.domain.user.dto.response.OAuthAccountResponse;
import com.backend.orbitflow.domain.user.enums.Provider;
import com.backend.orbitflow.domain.user.service.OAuthService;
import com.backend.orbitflow.domain.user.dto.request.*;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.dto.response.UserPasswordUpdateResult;
import com.backend.orbitflow.domain.user.error.UserErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.util.EmailService;
import com.backend.orbitflow.global.util.S3Service;
import com.backend.orbitflow.global.util.S3TransactionalFileManager;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.backend.orbitflow.domain.user.dto.response.UserResponse;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private static final String PROFILE_IMAGE_DIR = "profiles";

    private final UserService userService;
    private final AuthService authService;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final S3Service s3Service;
    private final S3TransactionalFileManager s3FileManager;
    private final OAuthService oAuthService;
    private final TeamService teamService;
    private final FollowService followService;
    private final BlockService blockService;
    private final AvatarService avatarService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserResponse signup(UserSignupRequest request) {
        authService.consumeVerifyToken(request.email(), request.emailVerifyToken());
        releaseWithdrawnEmail(request.email());
        User user = userService.register(request.email(), authService.encodePassword(request.password()), request.name());
        avatarService.createAvatar(user);
        return UserResponse.from(user);
    }

    private void releaseWithdrawnEmail(String email) {
        userService.releaseWithdrawnEmail(email).ifPresent(oAuthService::unlinkAll);
    }

    public UserResponse getUser(AuthUser authUser) {
        return UserResponse.from(userService.getActiveByUuid(authUser.getUuid()));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(AuthUser authUser, String userUuid) {
        User viewer = userService.getActiveByUuid(authUser.getUuid());
        User target = userService.getByUuid(userUuid);
        User viewable = userService.getViewableUser(target, blockService.isBlocked(viewer, target));
        return UserProfileResponse.from(
                viewable,
                followService.getFollowState(viewer, viewable),
                followService.getFollowState(viewable, viewer)
        );
    }

    public UserResponse updateProfile(AuthUser authUser, UserProfileUpdateRequest request) {
        return UserResponse.from(userService.updateProfile(
                authUser.getUuid(), request.name(), request.introduce(), request.isPrivate()
        ));
    }

    public UserResponse updateProfileImage(AuthUser authUser, MultipartFile image) {
        String imageUrl = s3Service.uploadFile(PROFILE_IMAGE_DIR, image);
        try {
            return UserResponse.from(userService.replaceProfileImage(authUser.getUuid(), imageUrl));
        } catch (RuntimeException e) {
            s3FileManager.deleteAfterCommit(List.of(imageUrl));
            throw e;
        }
    }
    public UserResponse deleteProfileImage(AuthUser authUser) {
        return UserResponse.from(userService.replaceProfileImage(authUser.getUuid(), null));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserResponse updateEmail(AuthUser authUser, UserEmailUpdateRequest request) {
        authService.consumeVerifyToken(request.email(), request.emailVerifyToken());
        releaseWithdrawnEmail(request.email());
        return UserResponse.from(userService.updateEmail(authUser.getUuid(), request.email()));
    }

    @Transactional
    public UserPasswordUpdateResult updatePassword(AuthUser authUser, UserPasswordUpdateRequest request) {
        User user = userService.getActiveByUuid(authUser.getUuid());
        if (user.getPassword() == null) {
            throw new CommonException(UserErrorCode.PASSWORD_NOT_SET);
        }
        authService.verifyPassword(request.password(), user.getPassword());
        if (request.password().equals(request.newPassword())) {
            throw new CommonException(UserErrorCode.SAME_AS_CURRENT_PASSWORD);
        }
        User updated = userService.updatePassword(authUser.getUuid(), authService.encodePassword(request.newPassword()));
        emailService.sendPasswordChangedEmail(updated.getEmail(), updated.getName());
        return new UserPasswordUpdateResult(
                UserResponse.from(updated),
                tokenService.createRefreshToken(updated.getUuid())
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserResponse setInitialPassword(AuthUser authUser, UserPasswordSetRequest request) {
        return UserResponse.from(userService.setInitialPassword(
                authUser.getUuid(), authService.encodePassword(request.newPassword())
        ));
    }

    @Transactional(readOnly = true)
    public List<OAuthAccountResponse> getOAuthAccounts(AuthUser authUser) {
        return oAuthService.getLinkedAccounts(userService.getActiveByUuid(authUser.getUuid())).stream()
                .map(OAuthAccountResponse::from)
                .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void unlinkOAuthAccount(AuthUser authUser, Provider provider) {
        User locked = userService.lockUser(userService.getActiveByUuid(authUser.getUuid()).getId());
        oAuthService.unlink(locked, provider);
    }

    @Transactional
    public UserResponse updateChatInviteSetting(AuthUser authUser, UserChatInviteRequest request) {
        return UserResponse.from(userService.updateChatInviteSetting(authUser.getUuid(), request.allowNonFollowChatInvite()));
    }

    @Transactional
    public void deleteUser(AuthUser authUser, UserDeleteRequest request) {
        User user = userService.getActiveByUuid(authUser.getUuid());

        if (teamService.ownsActiveTeam(user)) {
            throw new CommonException(UserErrorCode.TEAM_OWNER_CANNOT_WITHDRAW);
        }
        if (user.getPassword() != null) {
            if (request.password() == null) {
                throw new CommonException(UserErrorCode.PASSWORD_REQUIRED);
            }
            authService.verifyPassword(request.password(), user.getPassword());
        } else if (!user.getName().equals(request.confirmName())) {
            throw new CommonException(UserErrorCode.WRONG_USER_NAME);
        }
        userService.deleteUser(user.getUuid());
        tokenService.deleteRefreshToken(user.getUuid());
        emailService.sendWithdrawalEmail(user.getEmail(), user.getName());
    }
}
