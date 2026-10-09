package com.backend.orbitflow.global.advice;

import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.error.ErrorCode;
import com.backend.orbitflow.global.error.GlobalErrorCode;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// /error 처리 : 기본 HTML 오류 페이지(Whitelabel) 대신 항상 공통 응답 형식(JSON)으로 반환
// 컨트롤러 밖(필터 등)에서 난 예외, 없는 경로(404), 지원하지 않는 메서드(405) 등이 여기로 전달됨
@RestController
public class ErrorResponseController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<CommonResponse<Object>> error(HttpServletRequest request) {
        Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttribute instanceof Integer code ? code : 500;
        ErrorCode errorCode = switch (status) {
            case 400 -> GlobalErrorCode.INVALID_INPUT_VALUE;
            case 401 -> GlobalErrorCode.UNAUTHORIZED;
            case 403 -> GlobalErrorCode.ACCESS_DENIED;
            case 404 -> GlobalErrorCode.RESOURCE_NOT_FOUND;
            case 405 -> GlobalErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> GlobalErrorCode.INTERNAL_SERVER_ERROR;
        };
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(CommonResponse.fail(null, errorCode, uri == null ? request.getRequestURI() : uri.toString()));
    }
}
