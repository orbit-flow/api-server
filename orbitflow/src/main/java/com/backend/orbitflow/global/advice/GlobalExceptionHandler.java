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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
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

    // 본문 누락·JSON 문법 오류·필드 타입 불일치
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Object>> handleInvalidRequest(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        return invalidRequest(InvalidRequestException.from(e), request);
    }

    // 쿼리 파라미터·경로 변수 타입 불일치
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonResponse<Object>> handleInvalidRequest(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        return invalidRequest(InvalidRequestException.from(e), request);
    }

    // 필수 쿼리 파라미터·헤더·쿠키 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonResponse<Object>> handleInvalidRequest(
            MissingServletRequestParameterException e,
            HttpServletRequest request
    ) {
        return invalidRequest(InvalidRequestException.of(e.getParameterName(), "필수 파라미터입니다."), request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<CommonResponse<Object>> handleInvalidRequest(
            MissingRequestHeaderException e,
            HttpServletRequest request
    ) {
        return invalidRequest(InvalidRequestException.of(e.getHeaderName(), "필수 헤더입니다."), request);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<CommonResponse<Object>> handleInvalidRequest(
            MissingRequestCookieException e,
            HttpServletRequest request
    ) {
        return invalidRequest(InvalidRequestException.of(e.getCookieName(), "필수 쿠키입니다."), request);
    }

    // multipart 필수 파트 누락
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<CommonResponse<Object>> handleInvalidRequest(
            MissingServletRequestPartException e,
            HttpServletRequest request
    ) {
        return invalidRequest(InvalidRequestException.of(e.getRequestPartName(), "필수 파트입니다."), request);
    }

    // 업로드 크기 제한 초과
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<CommonResponse<Object>> handleMaxUploadSize(
            MaxUploadSizeExceededException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = GlobalErrorCode.FILE_SIZE_EXCEED;
        log.warn("업로드 크기 초과: {}", request.getRequestURI());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(CommonResponse.fail(null, errorCode, request.getRequestURI()));
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
