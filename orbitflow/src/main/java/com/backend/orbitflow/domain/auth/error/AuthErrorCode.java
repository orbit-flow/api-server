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
            "Email Varify Fail"),
    EMAIL_SEND_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS,
            "인증 메일은 1분에 한 번만 요청할 수 있습니다. 잠시 후 다시 시도해 주세요.",
            "https://orbitflow.com/errors/email-send-cooldown",
            "Email Send Cooldown"),
    EMAIL_SEND_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS,
            "인증 메일 발송 횟수를 초과했습니다. 1시간 후 다시 시도해 주세요.",
            "https://orbitflow.com/errors/email-send-limit-exceeded",
            "Email Send Limit Exceeded"),
    EMAIL_VARIFY_TOO_MANY_FAILS(HttpStatus.TOO_MANY_REQUESTS,
            "인증 번호를 여러 번 잘못 입력했습니다. 인증 번호를 다시 요청해 주세요.",
            "https://orbitflow.com/errors/email-varify-too-many-fails",
            "Email Varify Too Many Fails"),
    ACCOUNT_DORMANT(HttpStatus.FORBIDDEN,
            "장기 미접속으로 휴면 전환된 계정입니다. 이메일 인증 후 이용할 수 있습니다.",
            "https://orbitflow.com/errors/account-dormant",
            "Account Dormant"),
    NOT_DORMANT_ACCOUNT(HttpStatus.BAD_REQUEST,
            "휴면 계정이 아닙니다.",
            "https://orbitflow.com/errors/not-dormant-account",
            "Not Dormant Account"),
    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.BAD_REQUEST,
            "비밀번호 재설정 링크가 만료되었거나 이미 사용되었습니다. 다시 요청해 주세요.",
            "https://orbitflow.com/errors/invalid-password-reset-token",
            "Invalid Password Reset Token"),
    PASSWORD_RESET_TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS,
            "비밀번호 재설정 메일은 1분에 한 번만 요청할 수 있습니다. 잠시 후 다시 시도해 주세요.",
            "https://orbitflow.com/errors/password-reset-too-many-requests",
            "Password Reset Too Many Requests");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
