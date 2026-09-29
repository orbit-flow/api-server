package com.backend.orbitflow.domain.user.dto.response;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSuccessCode {

    USER_SIGNUP_SUCCESS(HttpStatus.CREATED, "회원가입 되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
