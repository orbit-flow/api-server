package com.backend.orbitflow.global.advice;

import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.error.ErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<CommonResponse<Object>> handleGlobalException(
            CommonException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = e.getErrorCode();

        String instance = request.getRequestURI();

        log.error("예외 발생: [{}] - {}", errorCode.getTitle(), errorCode.getMessage(), e);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(CommonResponse.fail(e.getData(), errorCode, instance));
    }
}
