package com.backend.orbitflow.domain.auth.controller;

import com.backend.orbitflow.domain.auth.dto.request.DormantReleaseRequest;
import com.backend.orbitflow.domain.auth.dto.request.PasswordResetMailRequest;
import com.backend.orbitflow.domain.auth.dto.request.PasswordResetRequest;
import com.backend.orbitflow.domain.auth.dto.request.EmailVarifyRequest;
import com.backend.orbitflow.domain.auth.dto.request.LoginRequest;
import com.backend.orbitflow.domain.auth.dto.request.EmailCodeRequest;
import com.backend.orbitflow.domain.auth.dto.response.AuthSuccessCode;
import com.backend.orbitflow.domain.auth.dto.response.EmailCodeResponse;
import com.backend.orbitflow.domain.auth.dto.response.EmailVarifyResponse;
import com.backend.orbitflow.domain.auth.dto.response.TokenResponse;
import com.backend.orbitflow.domain.auth.facade.AuthFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.backend.orbitflow.global.security.JwtProvider.AUTHORIZATION_HEADER;
import static com.backend.orbitflow.global.security.JwtProvider.REFRESH_HEADER;
import com.backend.orbitflow.domain.auth.dto.response.LoginResponse;
import com.backend.orbitflow.domain.auth.dto.response.LoginResult;
import com.backend.orbitflow.domain.suspension.dto.response.SuspendedAccountResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthFacade authFacade;

    // access token은 Authorization 헤더(토큰 재발급과 동일), refresh token은 HttpOnly 쿠키로 전달
    // 정지 계정은 토큰 없이 data.suspended = true와 정지 안내 정보만 반환
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResult result = authFacade.login(request);
        if (result.isSuspended()) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(CommonResponse.success(
                            AuthSuccessCode.LOGIN_SUSPENDED,
                            result.response()
                    ));
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(AUTHORIZATION_HEADER, result.token().accessToken())
                .header(HttpHeaders.SET_COOKIE, result.token().refreshToken().toString())
                .body(CommonResponse.success(
                        AuthSuccessCode.LOGIN_SUCCESS,
                        result.response()
                ));
    }

    // 소셜 로그인 정지 안내 : FE /suspended?ticket=... 화면에서 정지 사유·기간 조회 (10분간 유효)
    @GetMapping("/suspension-notice")
    public ResponseEntity<CommonResponse<SuspendedAccountResponse>> getSuspensionNotice(
            @RequestParam String ticket
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AuthSuccessCode.SUSPENSION_NOTICE,
                        authFacade.getSuspensionNotice(ticket)
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestHeader(AUTHORIZATION_HEADER) String accessToken
    ) {
        authFacade.logout(authUser, accessToken);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AuthSuccessCode.LOGOUT_SUCCESS
                ));
    }

    @PostMapping("/token-reissue")
    public ResponseEntity<CommonResponse<Void>> reissue (
            @CookieValue(name = REFRESH_HEADER) String refreshToken
    ) {
        TokenResponse token = authFacade.reissueToken(refreshToken);
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(AUTHORIZATION_HEADER, token.accessToken())
                .header(HttpHeaders.SET_COOKIE, token.refreshToken().toString())
                .body(CommonResponse.success(
                        AuthSuccessCode.REISSUE_SUCCESS
                ));
    }

    // 비밀번호 분실 : 재설정 링크 메일 발송 (가입 여부와 무관하게 같은 응답)
    @PostMapping("/password/reset-mail")
    public ResponseEntity<CommonResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetMailRequest request
    ) {
        authFacade.requestPasswordReset(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AuthSuccessCode.PASSWORD_RESET_MAIL_SENT
                ));
    }

    // 재설정 링크(FE /reset-password?token=...)에서 새 비밀번호 저장
    @PostMapping("/password/reset")
    public ResponseEntity<CommonResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        authFacade.resetPassword(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AuthSuccessCode.PASSWORD_RESET_SUCCESS
                ));
    }

    // 장기 미접속 휴면 계정 해제 (일회성 이메일 인증)
    @PostMapping("/dormant/release")
    public ResponseEntity<CommonResponse<Void>> releaseDormant(
            @Valid @RequestBody DormantReleaseRequest request
    ) {
        authFacade.releaseDormant(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AuthSuccessCode.DORMANT_RELEASE
                ));
    }

    @PostMapping("/email/sendcode")
    public ResponseEntity<CommonResponse<EmailCodeResponse>> sendCode(
            @Valid @RequestBody EmailCodeRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AuthSuccessCode.EMAIL_CODE_SEND,
                        authFacade.sendCode(request)
                ));
    }

    @PostMapping("/email/varify")
    public ResponseEntity<CommonResponse<EmailVarifyResponse>> varifyEmail(
            @Valid @RequestBody EmailVarifyRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AuthSuccessCode.EMAIL_VARIFY_SUCCESS,
                        authFacade.varifyEmail(request)
                ));
    }
}
