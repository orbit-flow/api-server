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
            "Auth Invalid Credentials");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
