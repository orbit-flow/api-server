package com.backend.orbitflow.global.advice;

import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.error.ErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.common.error.exception.InvalidRequestException;

import com.backend.orbitflow.global.error.GlobalErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.PessimisticLockingFailureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
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

    // 요청 검증(@Valid) 실패 : 필드별 오류 목록을 data에 담아 공통 응답 형식으로 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Object>> handleInvalidRequest(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        return invalidRequest(InvalidRequestException.from(e.getBindingResult()), request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<CommonResponse<Object>> handleInvalidRequest(
            HandlerMethodValidationException e,
            HttpServletRequest request
    ) {
        return invalidRequest(InvalidRequestException.from(e), request);
    }

    // 사용자 입력 오류이므로 스택 트레이스 없이 경고로만 기록
    private ResponseEntity<CommonResponse<Object>> invalidRequest(InvalidRequestException e, HttpServletRequest request) {
        log.warn("요청 검증 실패: {} - {}", request.getRequestURI(), e.getData());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(CommonResponse.fail(e.getData(), e.getErrorCode(), request.getRequestURI()));
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
