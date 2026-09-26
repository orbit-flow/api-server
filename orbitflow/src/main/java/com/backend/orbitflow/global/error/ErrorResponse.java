package com.backend.orbitflow.global.error;

public record ErrorResponse(
        String type,
        String title,
        String instance
) {

    public static ErrorResponse of(
        String type,
        String title,
        String instance
    ) {
        return new ErrorResponse(
                type,
                title,
                instance
        );
    }
}
