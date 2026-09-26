package com.backend.orbitflow.global.error.exception;

import com.backend.orbitflow.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException{

    private final ErrorCode errorCode;
    private final Object data;

    public GlobalException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = null;
    }

    public GlobalException(
            ErrorCode errorCode,
            Object data
    ) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = data;
    }
}
