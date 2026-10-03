package com.backend.orbitflow.domain.user.facade;

import com.backend.orbitflow.domain.auth.service.AuthService;
import com.backend.orbitflow.domain.user.dto.request.*;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.backend.orbitflow.domain.user.dto.response.UserResponse;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final AuthService authService;

    public UserResponse signup(UserSignupRequest request) {
        String encodedPassword = authService.encodePassword(request.password());
        return UserResponse.from(userService.register(request.email(), encodedPassword, request.name(), request.emailVarifyToken()));
    }

    public UserResponse getUser(AuthUser authUser) {
        return UserResponse.from(userService.getByUuid(authUser.getUuid()));
    }

    public UserResponse updateProfile(AuthUser authUser, UserProfileUpdateRequest request) {
        return UserResponse.from(userService.updateProfile(
                authUser.getUuid(), request.name(), request.profileImage(), request.introduce(), request.isPrivate()
        ));
    }

    public UserResponse updateEmail(AuthUser authUser, UserEmailUpdateRequest request) {
        return UserResponse.from(userService.updateEmail(
                authUser.getUuid(), request.email()
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

    public void deleteUser(AuthUser authUser, UserDeleteRequest request) {
        userService.deleteUser(authUser.getUuid(), request.confirmName());
    }
}
