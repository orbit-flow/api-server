package com.backend.orbitflow.domain.user.facade;

import com.backend.orbitflow.domain.auth.service.AuthService;
import com.backend.orbitflow.domain.auth.service.TokenService;
import com.backend.orbitflow.domain.user.dto.request.*;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.error.UserErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.util.EmailService;
import com.backend.orbitflow.global.util.S3Service;
import com.backend.orbitflow.global.util.S3TransactionalFileManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.backend.orbitflow.domain.user.dto.response.UserResponse;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
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

    // 기본 아바타·초기 포인트는 가입 트랜잭션에서 함께 생성 (UserRegisteredEvent)
    public UserResponse signup(UserSignupRequest request) {
        String encodedPassword = authService.encodePassword(request.password());
        return UserResponse.from(userService.register(request.email(), encodedPassword, request.name(), request.emailVarifyToken()));
    }

    public UserResponse getUser(AuthUser authUser) {
        return UserResponse.from(userService.getByUuid(authUser.getUuid()));
    }

    public UserResponse updateProfile(AuthUser authUser, UserProfileUpdateRequest request) {
        return UserResponse.from(userService.updateProfile(
                authUser.getUuid(), request.name(), request.introduce(), request.isPrivate()
        ));
    }

    // 업로드 실패·롤백 시 새 파일 삭제, 커밋 후 이전 파일 삭제 (소셜 프로필 등 외부 URL은 삭제하지 않음)
    @Transactional
    public UserResponse updateProfileImage(AuthUser authUser, MultipartFile image) {
        String imageUrl = s3FileManager.upload(PROFILE_IMAGE_DIR, image);
        String oldImage = userService.updateProfileImage(authUser.getUuid(), imageUrl);
        deleteManagedImage(oldImage);
        return UserResponse.from(userService.getByUuid(authUser.getUuid()));
    }

    // 기본 이미지로 초기화
    @Transactional
    public UserResponse deleteProfileImage(AuthUser authUser) {
        String oldImage = userService.updateProfileImage(authUser.getUuid(), null);
        deleteManagedImage(oldImage);
        return UserResponse.from(userService.getByUuid(authUser.getUuid()));
    }

    public UserResponse updateEmail(AuthUser authUser, UserEmailUpdateRequest request) {
        return UserResponse.from(userService.updateEmail(
                authUser.getUuid(), request.email(), request.emailVarifyToken()
        ));
    }

    @Transactional
    public UserResponse updatePassword(AuthUser authUser, UserPasswordUpdateRequest request) {
        User user = userService.getByUuid(authUser.getUuid());
        authService.verifyPassword(request.password(), user.getPassword());
        return UserResponse.from(
                userService.updatePassword(
                        authUser.getUuid(),
                        authService.encodePassword(request.newPassword())
                ));
    }

    @Transactional
    public UserResponse updateChatInviteSetting(AuthUser authUser, UserChatInviteRequest request) {
        return UserResponse.from(userService.updateChatInviteSetting(authUser.getUuid(), request.allowNonFollowChatInvite()));
    }

    // 이메일·비밀번호 가입자는 현재 비밀번호 재입력, 비밀번호가 없는 소셜 가입자는 이름 확인
    // 정지 계정은 조회 단계에서 제외되어 탈퇴 불가 (정지 해제 후 탈퇴)
    // 탈퇴 즉시 세션 종료(refresh token 폐기), 결과는 가입 이메일로 고지 (메일 실패는 탈퇴 결과에 영향 없음)
    public void deleteUser(AuthUser authUser, UserDeleteRequest request) {
        User user = userService.getByUuid(authUser.getUuid());
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
        try {
            emailService.sendWithdrawalEmail(user.getEmail(), user.getName());
        } catch (RuntimeException e) {
            log.warn("탈퇴 안내 메일 발송 실패 : {}", user.getUuid(), e);
        }
    }

    private void deleteManagedImage(String imageUrl) {
        if (s3Service.isManagedFile(imageUrl)) {
            s3FileManager.deleteAfterCommit(List.of(imageUrl));
        }
    }
}
