package com.backend.orbitflow.domain.timeline.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TimelineErrorCode implements ErrorCode {

    INVALID_CURSOR(HttpStatus.BAD_REQUEST,
            "잘못된 타임라인 커서입니다.",
            "https://orbitflow.com/errors/invalid-cursor",
            "Invalid Cursor");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
