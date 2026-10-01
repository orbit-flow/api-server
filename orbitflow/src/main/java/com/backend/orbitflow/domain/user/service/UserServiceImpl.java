package com.backend.orbitflow.domain.user.service;

import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.UserRole;
import com.backend.orbitflow.domain.user.error.UserErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.orbitflow.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getByUuid(String uuid) {
        return userRepository.findByUuid(uuid).orElseThrow(
                () -> new CommonException(UserErrorCode.USER_NOT_FOUND)
        );
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new CommonException(UserErrorCode.USER_NOT_FOUND)
        );
    }

    public User register(String email, String password, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new CommonException(UserErrorCode.EMAIL_DUPLICATE);
        }
        User user = User.of(
                UUID.randomUUID().toString().replace("-", ""),
                email,
                password,
                name,
                UserRole.ROLE_USER
        );
        return userRepository.save(user);
    }

    public User updateProfile(String uuid, String name, String profileImage, String introduce, boolean isPrivate) {
        User user = getByUuid(uuid);
        user.updateUserInfo(name, profileImage, introduce, isPrivate);
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
}
