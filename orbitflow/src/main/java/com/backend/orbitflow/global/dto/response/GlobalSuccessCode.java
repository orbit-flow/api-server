package com.backend.orbitflow.global.dto.response;

import org.springframework.http.HttpStatus;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter 
@RequiredArgsConstructor 
public enum GlobalSuccessCode implements SuccessCode{

    REQUEST_SUCCESS(HttpStatus.OK , "정상적으로 처리되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
