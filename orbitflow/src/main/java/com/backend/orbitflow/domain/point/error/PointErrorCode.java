package com.backend.orbitflow.domain.point.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PointErrorCode implements ErrorCode {

    ALREADY_ATTENDED(HttpStatus.CONFLICT,
            "오늘은 이미 출석했습니다.",
            "https://orbitflow.com/errors/already-attended",
            "Already Attended");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
