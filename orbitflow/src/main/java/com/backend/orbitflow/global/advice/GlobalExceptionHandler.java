package com.backend.orbitflow.global.advice;

import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.error.ErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;

import com.backend.orbitflow.global.error.GlobalErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.PessimisticLockingFailureException;
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

    // 비관적 락 대기 시간 초과·데드락 : 같은 자원(포인트·결제 등)에 대한 동시 요청 충돌
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<CommonResponse<Object>> handleLockFailure(
            PessimisticLockingFailureException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = GlobalErrorCode.CONCURRENT_REQUEST_CONFLICT;
        log.warn("락 획득 실패: {} - {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(CommonResponse.fail(null, errorCode, request.getRequestURI()));
    }
}
