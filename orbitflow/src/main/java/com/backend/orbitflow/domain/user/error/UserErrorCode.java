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
            "Bad User Role"),
    EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST,
            "이미 가입된 이메일 주소입니다.",
            "https://orbitflow.com/errors/email-duplicate",
            "Email Duplicate"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 사용자입니다.",
            "https://orbitflow.com/errors/user-not-found",
            "User Not Found"),
    WRONG_USER_NAME(HttpStatus.BAD_REQUEST,
            "잘못된 사용자 이름입니다.",
            "https://orbitflow.com/errors/wrong-user-name",
            "Wrong User Name");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
