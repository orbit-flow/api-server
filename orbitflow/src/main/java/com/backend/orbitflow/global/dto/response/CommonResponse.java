package com.backend.orbitflow.global.dto.response;

import com.backend.orbitflow.global.error.ErrorCode;
import com.backend.orbitflow.global.error.ErrorResponse;

import java.time.Instant;

public record CommonResponse<T>(
        boolean success,
        int status,
        T data,
        ErrorResponse error,
        String message,
        Meta meta
) {

    public static <T> CommonResponse<T> success(
            ResponseCode responseCode,
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
            ResponseCode responseCode
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
                ErrorResponse.of(
                        errorCode.getType(),
                        errorCode.getTitle(),
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
            return new Meta(
                    Instant.now(),
                    traceId,
                    version
            );
        }
    }
}
