package com.backend.orbitflow.domain.user.facade;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.backend.orbitflow.domain.user.dto.request.UserEmailUpdateRequest;
import com.backend.orbitflow.domain.user.dto.request.UserPasswordUpdateRequest;
import com.backend.orbitflow.domain.user.dto.request.UserProfileUpdateRequest;
import com.backend.orbitflow.domain.user.dto.request.UserSignupRequest;
import com.backend.orbitflow.domain.user.dto.response.UserResponse;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;

    public UserResponse signup(UserSignupRequest request) {
        return null;
    }

    public UserResponse getUser(AuthUser authUser) {
        return UserResponse.of(userService.getUserByUuid(authUser.getUuid()));
    }

    public UserResponse updateProfile(AuthUser authUser, UserProfileUpdateRequest request) {
        return null;
    }

    public UserResponse updateEmail(AuthUser authUser, UserEmailUpdateRequest request) {
        return null;
    }

    public UserResponse updatePassword(AuthUser authUser, UserPasswordUpdateRequest request) {
        return null;
    }

    public Void deleteUser(AuthUser authUser) {
        return null;
    }
}
