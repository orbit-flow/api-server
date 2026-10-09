package com.backend.orbitflow.domain.user.controller;

import com.backend.orbitflow.domain.user.dto.request.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.orbitflow.domain.user.dto.response.UserResponse;
import com.backend.orbitflow.domain.user.dto.response.UserSuccessCode;
import com.backend.orbitflow.domain.user.facade.UserFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserFacade userFacade;

    @PostMapping
    public ResponseEntity<CommonResponse<UserResponse>> signUp (
        @Valid @RequestBody UserSignupRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(CommonResponse.success(
                UserSuccessCode.USER_SIGNUP_SUCCESS,
                userFacade.signup(request)
            ));
    }
    
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<UserResponse>> getUser(
        @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.GET_USER_INFO,
                userFacade.getUser(authUser)
            ));
    }

    @PutMapping("me/profile")
    public ResponseEntity<CommonResponse<UserResponse>> updateProfile(
        @AuthenticationPrincipal AuthUser authUser,
        @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.USER_PROFILE_UPDATE,
                userFacade.updateProfile(authUser, request)
            ));
    }

    // multipart : image (최대 10MB)
    @PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<UserResponse>> updateProfileImage(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.USER_PROFILE_IMAGE_UPDATE,
                userFacade.updateProfileImage(authUser, image)
            ));
    }

    // 기본 이미지로 초기화
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<CommonResponse<UserResponse>> deleteProfileImage(
        @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.USER_PROFILE_IMAGE_UPDATE,
                userFacade.deleteProfileImage(authUser)
            ));
    }

    @PutMapping("/me/email")
    public ResponseEntity<CommonResponse<UserResponse>> updateEmail(
        @AuthenticationPrincipal AuthUser authUser,
        @Valid @RequestBody UserEmailUpdateRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.USER_EMAIL_UPDATE,
                userFacade.updateEmail(authUser, request)
            ));
    }
    
    @PutMapping("/me/password")
    public ResponseEntity<CommonResponse<UserResponse>> updatePassword(
        @AuthenticationPrincipal AuthUser authUser,
        @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.USER_PASSWORD_UPDATE,
                userFacade.updatePassword(authUser, request)
            ));
    }

    // 팔로우 중이 아닌 사용자의 대화 초대 허용 여부
    @PatchMapping("/me/chat-invite")
    public ResponseEntity<CommonResponse<UserResponse>> updateChatInviteSetting(
        @AuthenticationPrincipal AuthUser authUser,
        @Valid @RequestBody UserChatInviteRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.USER_PROFILE_UPDATE,
                userFacade.updateChatInviteSetting(authUser, request)
            ));
    }

    @DeleteMapping("/me")
    public ResponseEntity<CommonResponse<Void>> deleteUser(
        @AuthenticationPrincipal AuthUser authUser,
        @Valid @RequestBody UserDeleteRequest request
    ) {
        userFacade.deleteUser(authUser, request);
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .body(CommonResponse.success(
                UserSuccessCode.USER_DELETE
            ));
    }
}
