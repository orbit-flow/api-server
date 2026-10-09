package com.backend.orbitflow.domain.user.service;

import com.backend.orbitflow.domain.auth.error.AuthErrorCode;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.UserRole;
import com.backend.orbitflow.domain.user.enums.UserStatus;
import com.backend.orbitflow.domain.user.error.UserErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.util.RedisUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.orbitflow.domain.user.repository.UserRepository;

import com.backend.orbitflow.domain.user.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService{

    private static final int WITHDRAWAL_RETENTION_DAYS = 30;
    private static final int DORMANT_MONTHS = 12;

    private final UserRepository userRepository;
    private final RedisUtil redisUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public User getByUuid(String uuid) {
        return userRepository.findByUuidAndDeletedAtIsNullAndStatusNot(uuid, UserStatus.BANNED).orElseThrow(
                () -> new CommonException(UserErrorCode.USER_NOT_FOUND)
        );
    }

    // 정지 계정 포함 조회 (관리자 정지 처리용)
    @Transactional(readOnly = true)
    public User getByUuidIncludingBanned(String uuid) {
        return userRepository.findByUuidAndDeletedAtIsNull(uuid).orElseThrow(
                () -> new CommonException(UserErrorCode.USER_NOT_FOUND)
        );
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new CommonException(UserErrorCode.USER_NOT_FOUND)
        );
    }

    public User register(String email, String password, String name, String varifyToken) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new CommonException(UserErrorCode.EMAIL_DUPLICATE);
        }
        String code = redisUtil.getValues(email, String.class).orElseThrow(
                () -> new CommonException(UserErrorCode.UN_VARIFIED_EMAIL)
        );
        if (!code.equals(varifyToken)) {
            throw new CommonException(UserErrorCode.UN_VARIFIED_EMAIL);
        }
        redisUtil.deleteValues(email);
        User user = User.of(
                UUID.randomUUID().toString().replace("-", ""),
                email,
                password,
                name,
                UserRole.ROLE_USER
        );
        return saveNewUser(user);
    }

    public User registerSocialUser(String email, String name, String profileImage) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new CommonException(UserErrorCode.EMAIL_DUPLICATE);
        }
        User user = User.social(
                UUID.randomUUID().toString().replace("-", ""),
                email,
                name,
                profileImage
        );
        return saveNewUser(user);
    }

    public User updateProfile(String uuid, String name, String introduce, boolean isPrivate) {
        User user = getByUuid(uuid);
        user.updateUserInfo(name, introduce, isPrivate);
        return userRepository.save(user);
    }

    // 변경 전 이미지 URL 반환 (호출 측에서 커밋 후 정리)
    public String updateProfileImage(String uuid, String profileImage) {
        User user = getByUuid(uuid);
        String oldImage = user.getProfileImage();
        user.updateProfileImage(profileImage);
        return oldImage;
    }

    // 이메일 변경은 새 이메일의 OTP 인증 토큰이 필요 (가입과 동일)
    public User updateEmail(String uuid, String email, String varifyToken) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new CommonException(UserErrorCode.EMAIL_DUPLICATE);
        }
        String token = redisUtil.getValues(email, String.class).orElseThrow(
                () -> new CommonException(UserErrorCode.UN_VARIFIED_EMAIL)
        );
        if (!token.equals(varifyToken)) {
            throw new CommonException(UserErrorCode.UN_VARIFIED_EMAIL);
        }
        redisUtil.deleteValues(email);
        User user = getByUuid(uuid);
        user.updateEmail(email);
        return userRepository.save(user);
    }

    public User updateChatInviteSetting(String uuid, boolean allowNonFollowChatInvite) {
        User user = getByUuid(uuid);
        user.updateAllowNonFollowChatInvite(allowNonFollowChatInvite);
        return user;
    }

    public User updatePassword(String uuid, String password) {
        User user = getByUuid(uuid);
        user.updatePassword(password);
        return userRepository.save(user);
    }

    // 탈퇴 신청 시점부터 30일간 논리적 삭제 상태로 보관 (본인 확인은 호출 측에서 수행)
    public void deleteUser(String uuid) {
        User user = getByUuid(uuid);
        user.delete();
        userRepository.save(user);
    }

    // 탈퇴 유예 기간(30일) 내 로그인 시 계정 복구, 기간이 지났으면 존재하지 않는 계정으로 처리
    public void restoreIfWithdrawn(User user) {
        if (user.getDeletedAt() == null) {
            return;
        }
        if (user.getDeletedAt().isBefore(LocalDateTime.now().minusDays(WITHDRAWAL_RETENTION_DAYS))) {
            throw new CommonException(AuthErrorCode.LOGIN_FAIL);
        }
        user.restore();
    }

    public void validateNotDormant(User user) {
        if (user.getStatus() == UserStatus.SLEEP) {
            throw new CommonException(AuthErrorCode.ACCOUNT_DORMANT);
        }
    }

    // 일회성 이메일 인증을 마친 휴면 계정 재활성화 (재전환 방지를 위해 마지막 로그인 시각 갱신)
    public void releaseDormant(User user) {
        if (user.getStatus() != UserStatus.SLEEP) {
            throw new CommonException(AuthErrorCode.NOT_DORMANT_ACCOUNT);
        }
        user.updateUserStatus(UserStatus.ACTIVE);
        user.updateLastLoginAt();
        userRepository.save(user);
    }

    // 마지막 로그인으로부터 12개월이 지난 계정 휴면 전환 (스케줄러)
    public int convertDormantUsers() {
        return userRepository.convertToDormant(LocalDateTime.now().minusMonths(DORMANT_MONTHS));
    }

    // 로그인 시점에는 탈퇴 유예 계정도 대상이 되므로 uuid 재조회 없이 엔티티를 받음
    public void updateLastLoginAt(User user) {
        user.updateLastLoginAt();
        userRepository.save(user);
    }

    // 가입 이벤트로 같은 트랜잭션에서 기본 아바타·초기 포인트 지급 (실패 시 가입도 롤백)
    private User saveNewUser(User user) {
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(saved));
        return saved;
    }
}
