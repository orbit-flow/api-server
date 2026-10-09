package com.backend.orbitflow.domain.user.service;

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

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService{

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

    public User updateProfile(String uuid, String name, String profileImage, String introduce, boolean isPrivate) {
        User user = getByUuid(uuid);
        user.updateUserInfo(name, introduce, profileImage, isPrivate);
        return userRepository.save(user);
    }

    public User updateEmail(String uuid, String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new CommonException(UserErrorCode.EMAIL_DUPLICATE);
        }
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

    public void deleteUser(String uuid, String confirmName) {
        User user = getByUuid(uuid);
        if (!user.getName().equals(confirmName)) {
            throw new CommonException(UserErrorCode.WRONG_USER_NAME);
        }
        user.delete();
        userRepository.save(user);
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
