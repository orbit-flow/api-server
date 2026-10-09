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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.backend.orbitflow.domain.user.dto.response.OAuthAccountResponse;
import com.backend.orbitflow.domain.user.dto.response.UserPasswordUpdateResult;
import com.backend.orbitflow.domain.user.enums.Provider;
import java.util.List;
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
    
    // 소셜 가입자의 최초 비밀번호 설정 (설정 후 소셜 연결 해제 가능)
    @PostMapping("/me/password")
    public ResponseEntity<CommonResponse<UserResponse>> setInitialPassword(
        @AuthenticationPrincipal AuthUser authUser,
        @Valid @RequestBody UserPasswordSetRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.USER_PASSWORD_SET,
                userFacade.setInitialPassword(authUser, request)
            ));
    }

    @GetMapping("/me/oauth-accounts")
    public ResponseEntity<CommonResponse<List<OAuthAccountResponse>>> getOAuthAccounts(
        @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.GET_OAUTH_ACCOUNTS,
                userFacade.getOAuthAccounts(authUser)
            ));
    }

    // 비밀번호가 설정되어 있거나 다른 소셜 계정이 남아 있을 때만 해제 가능
    @DeleteMapping("/me/oauth-accounts/{provider}")
    public ResponseEntity<CommonResponse<Void>> unlinkOAuthAccount(
        @AuthenticationPrincipal AuthUser authUser,
        @PathVariable Provider provider
    ) {
        userFacade.unlinkOAuthAccount(authUser, provider);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.OAUTH_UNLINK
            ));
    }

    // 현재 비밀번호 확인 후 변경, 현재 기기에는 새 refresh token을 발급하고 다른 기기의 세션은 종료
    @PutMapping("/me/password")
    public ResponseEntity<CommonResponse<UserResponse>> updatePassword(
        @AuthenticationPrincipal AuthUser authUser,
        @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        UserPasswordUpdateResult result = userFacade.updatePassword(authUser, request);
        return ResponseEntity
            .status(HttpStatus.OK)
            .header(HttpHeaders.SET_COOKIE, result.refreshToken().toString())
            .body(CommonResponse.success(
                UserSuccessCode.USER_PASSWORD_UPDATE,
                result.user()
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
