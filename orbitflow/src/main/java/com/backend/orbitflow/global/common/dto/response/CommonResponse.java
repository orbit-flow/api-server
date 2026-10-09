package com.backend.orbitflow.global.common.dto.response;

import com.backend.orbitflow.global.common.error.ErrorCode;

import java.time.Instant;

public record CommonResponse<T>(
        boolean success,
        int status,
        T data,
        Error error,
        String message,
        Meta meta
) {

    public static <T> CommonResponse<T> success(
            SuccessCode responseCode,
            T data
    ) {
        return new CommonResponse<>(
                true,
                responseCode.getHttpStatus().value(),
                data,
                null,
                responseCode.getMessage(),
                null
        );
    }

    public static <T> CommonResponse<T> success(
            SuccessCode responseCode
    ) {
        return CommonResponse.success(
                responseCode,
                null
        );
    }

    public static <T> CommonResponse<T> fail(
            T data,
            ErrorCode errorCode,
            String instance
    ) {
        return new CommonResponse<>(
                false,
                errorCode.getHttpStatus().value(),
                data,
                Error.of(
                        errorCode.getTitle(),
                        errorCode.getType(),
                        instance
                ),
                errorCode.getMessage(),
                null
        );
    }

    public record Meta(
            Instant timestamp,
            String traceId,
            String version
    ) {
        public static Meta of(String traceId, String version) {
            return new Meta(Instant.now(), traceId, version
            );
        }
    }

    public record Error(
        String title,
        String type,
        String instance
    ) {
        public static Error of(String title, String type, String instance) {
                return new Error(title, type, instance);
        }
    }
}
