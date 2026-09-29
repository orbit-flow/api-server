package com.backend.orbitflow.domain.user.error;

import org.springframework.http.HttpStatus;

import com.backend.orbitflow.global.common.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter 
@RequiredArgsConstructor 
public enum UserErrorCode implements ErrorCode{

    BAD_USER_ROLE(HttpStatus.BAD_REQUEST,
            "잘못된 사용자 권한입니다.",
            "https://orbitflow.com/errors/bad-user-role",
            "Bad User Role");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
