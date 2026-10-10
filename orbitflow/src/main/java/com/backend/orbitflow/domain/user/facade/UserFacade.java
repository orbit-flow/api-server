package com.backend.orbitflow.domain.user.facade;

import com.backend.orbitflow.domain.auth.service.AuthService;
import com.backend.orbitflow.domain.auth.service.TokenService;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.backend.orbitflow.global.common.dto.response.PageResponse;

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
    private final PlatformTransactionManager transactionManager;

    // 기본 아바타·초기 포인트는 가입 트랜잭션에서 함께 생성 (UserRegisteredEvent)
    // 이메일 인증 토큰(Redis, 1회용)을 먼저 소비한 뒤 가입 : 인증하지 않은 요청에는 이메일 중복 여부가 드러나지 않음
    // 탈퇴한 계정이 점유한 이메일은 풀어 주고(재가입 허용) 새 가입까지 한 트랜잭션으로 처리, bcrypt 는 트랜잭션 밖에서 수행
    public UserResponse signup(UserSignupRequest request) {
        authService.consumeVerifyToken(request.email(), request.emailVarifyToken());
        String encodedPassword = authService.encodePassword(request.password());
        return new TransactionTemplate(transactionManager).execute(status -> {
            releaseWithdrawnEmail(request.email());
            return UserResponse.from(userService.register(request.email(), encodedPassword, request.name()));
        });
    }

    // 탈퇴한 계정(30일 유예 중)과 이메일이 같으면 그 계정의 식별정보·소셜 연결을 지워 이메일을 풀어 줌 (로그인은 복구, 가입은 새 계정)
    private void releaseWithdrawnEmail(String email) {
        userService.releaseWithdrawnEmail(email).ifPresent(oAuthService::unlinkAll);
    }

    public UserResponse getUser(AuthUser authUser) {
        return UserResponse.from(userService.getActiveByUuid(authUser.getUuid()));
    }

    // 다른 사용자 프로필 열람 : 대상은 휴면 계정도 포함하는 조회, 본인은 ACTIVE 전용 조회
    // 차단 관계(양방향)면 존재하지 않는 사용자로 응답하고, 팔로우 상태는 서로의 방향을 각각 조회
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

    // S3 업로드(외부 HTTP)는 DB 커넥션을 잡지 않도록 트랜잭션 밖에서 수행하고, 이미지 URL 교체만 짧은 트랜잭션으로 처리
    // 교체 실패·롤백 시 새 파일 삭제, 커밋 후 이전 파일 삭제 (소셜 프로필 등 외부 URL은 삭제하지 않음)
    public UserResponse updateProfileImage(AuthUser authUser, MultipartFile image) {
        String imageUrl = s3Service.uploadFile(PROFILE_IMAGE_DIR, image);
        try {
            return new TransactionTemplate(transactionManager).execute(status -> replaceProfileImage(authUser.getUuid(), imageUrl));
        } catch (RuntimeException e) {
            // 트랜잭션 밖에서 호출되므로 즉시 삭제됨
            s3FileManager.deleteAfterCommit(List.of(imageUrl));
            throw e;
        }
    }

    // 기본 이미지로 초기화
    @Transactional
    public UserResponse deleteProfileImage(AuthUser authUser) {
        return replaceProfileImage(authUser.getUuid(), null);
    }

    // DB 작업은 사용자 조회 SELECT 1회 + 변경 UPDATE 1회 (응답도 같은 엔티티로 생성), 반드시 트랜잭션 안에서 호출
    private UserResponse replaceProfileImage(String uuid, String imageUrl) {
        User user = userService.getActiveByUuid(uuid);
        String oldImage = user.getProfileImage();
        userService.updateProfileImage(user, imageUrl);
        deleteManagedImage(oldImage);
        return UserResponse.from(user);
    }

    public UserResponse updateEmail(AuthUser authUser, UserEmailUpdateRequest request) {
        authService.consumeVerifyToken(request.email(), request.emailVarifyToken());
        return new TransactionTemplate(transactionManager).execute(status -> {
            releaseWithdrawnEmail(request.email());
            return UserResponse.from(userService.updateEmail(authUser.getUuid(), request.email()));
        });
    }

    // 현재 비밀번호 확인 후 변경 (소셜 전용 계정은 최초 설정 API 사용)
    // refresh token은 사용자당 1개이므로 현재 기기에 새로 발급하면 다른 기기의 세션은 갱신되지 않고 종료됨
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

    // 락 획득 후 확인(비밀번호 유무·남은 소셜 계정 수)이 동시 요청의 커밋을 보도록 READ COMMITTED (REPEATABLE READ 스냅샷 문제)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserResponse setInitialPassword(AuthUser authUser, UserPasswordSetRequest request) {
        return UserResponse.from(userService.setInitialPassword(
                authUser.getUuid(), authService.encodePassword(request.newPassword())
        ));
    }

    @Transactional(readOnly = true)
    public PageResponse<OAuthAccountResponse> getOAuthAccounts(AuthUser authUser, int page, int size) {
        return PageResponse.from(oAuthService.getLinkedAccounts(userService.getActiveByUuid(authUser.getUuid()), page, size)
                .map(OAuthAccountResponse::from));
    }

    // 락 획득 후 확인(비밀번호 유무·남은 소셜 계정 수)이 동시 요청의 커밋을 보도록 READ COMMITTED (REPEATABLE READ 스냅샷 문제)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void unlinkOAuthAccount(AuthUser authUser, Provider provider) {
        User locked = userService.lockUser(userService.getActiveByUuid(authUser.getUuid()).getId());
        oAuthService.unlink(locked, provider);
    }

    @Transactional
    public UserResponse updateChatInviteSetting(AuthUser authUser, UserChatInviteRequest request) {
        return UserResponse.from(userService.updateChatInviteSetting(authUser.getUuid(), request.allowNonFollowChatInvite()));
    }

    // 이메일·비밀번호 가입자는 현재 비밀번호 재입력, 비밀번호가 없는 소셜 가입자는 이름 확인
    // 정지 계정은 조회 단계에서 제외되어 탈퇴 불가 (정지 해제 후 탈퇴)
    // 탈퇴 즉시 세션 종료(refresh token 폐기), 결과는 가입 이메일로 비동기 고지 (@Async, 메일 실패는 탈퇴 결과에 영향 없음)
    public void deleteUser(AuthUser authUser, UserDeleteRequest request) {
        User user = userService.getActiveByUuid(authUser.getUuid());
        // 팀 소유자는 소유자 직함을 넘기거나 팀을 삭제한 뒤 탈퇴 (팀 탈퇴 정책과 동일)
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

    private void deleteManagedImage(String imageUrl) {
        if (s3Service.isManagedFile(imageUrl)) {
            s3FileManager.deleteAfterCommit(List.of(imageUrl));
        }
    }
}
