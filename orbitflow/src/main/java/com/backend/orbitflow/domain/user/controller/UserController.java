package com.backend.orbitflow.domain.user.controller;

import com.backend.orbitflow.domain.user.dto.request.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.orbitflow.domain.user.dto.response.UserProfileResponse;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User", description = "사용자 계정 및 프로필 API")
public class UserController {

    private final UserFacade userFacade;

    // 회원가입 메서드
    @PostMapping
    @Operation(summary = "회원가입", description = "이메일과 비밀번호를 사용해 새로운 사용자 계정을 생성합니다.")
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
    @Operation(summary = "내 정보 조회", description = "인증된 사용자의 계정 및 프로필 정보를 조회합니다.")
    public ResponseEntity<CommonResponse<UserResponse>> getUser(
        @Parameter(hidden = true)
        @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.GET_USER_INFO,
                userFacade.getUser(authUser)
            ));
    }

    // 다른 사용자 프로필 열람 (UUID·이름·소개·프로필 사진·팔로우 상태), /me 는 고정 경로가 우선 매칭됨
    @GetMapping("/{userUuid}")
    @Operation(summary = "다른 사용자 프로필 조회", description = "사용자 UUID로 다른 사용자의 프로필과 팔로우 상태를 조회합니다.")
    public ResponseEntity<CommonResponse<UserProfileResponse>> getUserProfile(
        @Parameter(hidden = true)
        @AuthenticationPrincipal AuthUser authUser,
        @Parameter(
            name = "userUuid",
            description = "조회할 사용자의 UUID",
            required = true,
            in = ParameterIn.PATH,
            example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @PathVariable("userUuid") String userUuid
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.GET_USER_PROFILE,
                userFacade.getUserProfile(authUser, userUuid)
            ));
    }

    @PutMapping("/me/profile")
    @Operation(summary = "내 프로필 수정", description = "인증된 사용자의 이름과 자기소개를 수정합니다.")
    public ResponseEntity<CommonResponse<UserResponse>> updateProfile(
        @Parameter(hidden = true)
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

    @PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 이미지 변경", description = "인증된 사용자의 프로필 이미지를 업로드합니다.")
    public ResponseEntity<CommonResponse<UserResponse>> updateProfileImage(
        @Parameter(hidden = true)
        @AuthenticationPrincipal AuthUser authUser,
        @Parameter(
            name = "image",
            description = "업로드할 프로필 이미지 파일",
            required = true
        )
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
    @Operation(summary = "프로필 이미지 삭제", description = "인증된 사용자의 프로필 이미지를 기본 이미지로 초기화합니다.")
    public ResponseEntity<CommonResponse<UserResponse>> deleteProfileImage(
        @Parameter(hidden = true)
        @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.USER_PROFILE_IMAGE_UPDATE,
                userFacade.deleteProfileImage(authUser)
            ));
    }

    // 이메일 수정
    @PutMapping("/me/email")
    @Operation(summary = "이메일 변경", description = "인증된 사용자의 이메일 주소를 변경합니다.")
    public ResponseEntity<CommonResponse<UserResponse>> updateEmail(
        @Parameter(hidden = true)
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
    @Operation(summary = "최초 비밀번호 설정", description = "소셜 로그인으로 가입한 사용자의 비밀번호를 최초로 설정합니다.")
    public ResponseEntity<CommonResponse<UserResponse>> setInitialPassword(
        @Parameter(hidden = true)
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

    // 소셜 계정 조회
    @GetMapping("/me/oauth-accounts")
    @Operation(summary = "연결된 소셜 계정 조회", description = "인증된 사용자에게 연결된 소셜 로그인 계정 목록을 조회합니다.")
    public ResponseEntity<CommonResponse<List<OAuthAccountResponse>>> getOAuthAccounts(
        @Parameter(hidden = true)
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
    @Operation(summary = "소셜 계정 연결 해제", description = "인증된 사용자의 특정 소셜 로그인 계정 연결을 해제합니다.")
    public ResponseEntity<CommonResponse<Void>> unlinkOAuthAccount(
        @Parameter(hidden = true)
        @AuthenticationPrincipal AuthUser authUser,
        @Parameter(
            name = "provider",
            description = "연결을 해제할 소셜 로그인 제공자",
            required = true,
            in = ParameterIn.PATH,
            example = "GOOGLE"
        )
        @PathVariable("provider") Provider provider
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
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다. 변경 후 현재 기기의 리프레시 토큰이 재발급됩니다.")
    public ResponseEntity<CommonResponse<UserResponse>> updatePassword(
        @Parameter(hidden = true)
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
    @Operation(summary = "대화 초대 설정 변경", description = "팔로우하지 않는 사용자의 대화 초대 허용 여부를 변경합니다.")
    public ResponseEntity<CommonResponse<UserResponse>> updateChatInviteSetting(
        @Parameter(hidden = true)
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
    @Operation(summary = "회원 탈퇴", description = "인증된 사용자의 계정을 탈퇴 처리합니다.")
    public ResponseEntity<CommonResponse<Void>> deleteUser(
        @Parameter(hidden = true)
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
