package com.backend.orbitflow.global.common.error.exception;

import com.backend.orbitflow.global.error.GlobalErrorCode;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.List;

// 요청 검증(@Valid) 실패 : 응답 data에 필드별 오류 목록을 담음
// 입력값(rejectedValue)은 비밀번호 등이 포함될 수 있어 응답에 싣지 않음
public class InvalidRequestException extends CommonException {

    public record FieldErrorDetail(String field, String message) {
    }

    public InvalidRequestException(List<FieldErrorDetail> errors) {
        super(GlobalErrorCode.INVALID_INPUT_VALUE, errors);
    }

    // @Valid @RequestBody, @Valid @RequestPart 검증 실패
    public static InvalidRequestException from(BindingResult bindingResult) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        bindingResult.getFieldErrors().forEach(error ->
                errors.add(new FieldErrorDetail(error.getField(), error.getDefaultMessage())));
        bindingResult.getGlobalErrors().forEach(error ->
                errors.add(new FieldErrorDetail(error.getObjectName(), error.getDefaultMessage())));
        return new InvalidRequestException(errors);
    }

    // 메서드 파라미터(@RequestParam·@PathVariable 등)에 선언한 제약 조건 검증 실패
    public static InvalidRequestException from(HandlerMethodValidationException exception) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        exception.getParameterValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String field = error instanceof FieldError fieldError ? fieldError.getField() : parameter;
                errors.add(new FieldErrorDetail(field, error.getDefaultMessage()));
            }
        });
        return new InvalidRequestException(errors);
    }
}
