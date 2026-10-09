package com.backend.orbitflow.domain.auth.controller;

import com.backend.orbitflow.domain.auth.dto.request.DormantReleaseRequest;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthFacade authFacade;

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<Void>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse token = authFacade.login(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, token.accessToken().toString())
                .header(HttpHeaders.SET_COOKIE, token.refreshToken().toString())
                .body(CommonResponse.success(
                        AuthSuccessCode.LOGIN_SUCCESS
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
