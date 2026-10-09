package com.backend.orbitflow.global.common.error.exception;

import com.backend.orbitflow.global.error.GlobalErrorCode;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// 요청 입력 오류(@Valid 검증 실패, 본문·파라미터 형식 오류, 필수 값 누락) : 응답 data에 필드별 오류 목록을 담음
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

    public static InvalidRequestException of(String field, String message) {
        return new InvalidRequestException(List.of(new FieldErrorDetail(field, message)));
    }

    // 요청 본문을 읽지 못함 : 본문 누락, JSON 문법 오류, 필드 타입 불일치(숫자 자리에 문자, 없는 enum 값 등)
    // field는 JSON 경로 (예: routine.durationType, items[0].id), 알 수 없으면 null
    public static InvalidRequestException from(HttpMessageNotReadableException exception) {
        // 날짜 파싱 실패처럼 Jackson 예외가 더 안쪽 원인을 감싸는 경우가 있어 원인 사슬 전체에서 탐색
        JacksonException jacksonException = null;
        for (Throwable cause = exception; cause != null && cause != cause.getCause(); cause = cause.getCause()) {
            if (cause instanceof DatabindException databindException && !databindException.getPath().isEmpty()) {
                Class<?> targetType = cause instanceof MismatchedInputException mismatched ? mismatched.getTargetType() : null;
                return of(path(databindException.getPath()), typeMessage(targetType));
            }
            if (cause instanceof JacksonException found && jacksonException == null) {
                jacksonException = found;
            }
        }
        if (jacksonException != null) {
            return of(null, "요청 본문의 JSON 형식이 올바르지 않습니다.");
        }
        return of(null, "요청 본문이 비어 있거나 읽을 수 없습니다.");
    }

    // 쿼리 파라미터·경로 변수의 타입 불일치
    public static InvalidRequestException from(MethodArgumentTypeMismatchException exception) {
        return of(exception.getName(), typeMessage(exception.getRequiredType()));
    }

    private static String typeMessage(Class<?> type) {
        if (type == null) {
            return "값의 형식이 올바르지 않습니다.";
        }
        if (type.isEnum()) {
            return "허용되지 않는 값입니다. 허용 값 : " + Arrays.stream(type.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
        }
        if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class)) {
            return "숫자여야 합니다.";
        }
        if (type == Boolean.class || type == boolean.class) {
            return "true 또는 false여야 합니다.";
        }
        if (TemporalAccessor.class.isAssignableFrom(type)) {
            return "날짜·시각 형식이 올바르지 않습니다. (예: 2026-10-09T09:00:00)";
        }
        return "값의 형식이 올바르지 않습니다.";
    }

    private static String path(List<JacksonException.Reference> references) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : references) {
            if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.isEmpty() ? null : path.toString();
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
