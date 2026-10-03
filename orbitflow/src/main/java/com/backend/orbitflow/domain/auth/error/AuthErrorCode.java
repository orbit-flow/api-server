package com.backend.orbitflow.domain.auth.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    AUTH_INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST,
            "잘못된 입력값입니다. 다시 시도해주세요.",
            "https://orbitflow.com/errors/auth-invalid-credentials",
            "Auth Invalid Credentials"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED,
            "잘못된 토큰입니다.",
            "https://orbitflow.com/errors/invalid-refresh-token",
            "Invalid Refresh Token"),
    LOGIN_FAIL(HttpStatus.BAD_REQUEST,
            "이메일 혹은 비밀번호가 잘못되었습니다.",
            "https://orbitflow.com/errors/login-fail",
            "Login Fail"),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST,
            "지원하지 않는 소셜 로그인 유형입니다.",
            "https://orbitflow.com/errors/upsupported-oauth-provider",
            "Unsupported OAuth Provider"),
    NO_VARIFY_CODE(HttpStatus.BAD_REQUEST,
            "이메일 인증 번호가 만료되었습니다.",
            "https://orbitflow.com/errors/no-varify-code",
            "No Varify Code"),
    EMAIL_VARIFY_FAIL(HttpStatus.BAD_REQUEST,
            "이메일 인증에 실패하였습니다. 다시 입력해주세요.",
            "https://orbitflow.com/errors/email-varify-fail",
            "Email Varify Fail");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
