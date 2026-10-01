package com.backend.orbitflow.domain.user.dto.response;

import org.springframework.http.HttpStatus;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSuccessCode implements SuccessCode{

    USER_SIGNUP_SUCCESS(HttpStatus.CREATED, "회원가입 되었습니다."),
    GET_USER_INFO(HttpStatus.OK, "내 정보가 열람되었습니다."),
    USER_PROFILE_UPDATE(HttpStatus.OK, "사용자 정보가 업데이트 되었습니다."),
    USER_EMAIL_UPDATE(HttpStatus.OK, "이메일이 업데이트 되었습니다."),
    USER_PASSWORD_UPDATE(HttpStatus.OK, "사용자 정보가 업데이트 되었습니다."),
    USER_DELETE(HttpStatus.NO_CONTENT, "회원 탈퇴가 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
