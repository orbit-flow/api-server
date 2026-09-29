package com.backend.orbitflow.global.common.error.exception;

import com.backend.orbitflow.global.common.error.ErrorCode;

import lombok.Getter;

@Getter
public class CommonException extends RuntimeException{

    private final ErrorCode errorCode;
    private final Object data;

    public CommonException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = null;
    }

    public CommonException(
            ErrorCode errorCode,
            Object data
    ) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = data;
    }
}
