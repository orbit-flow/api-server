package com.backend.orbitflow.domain.timeline.dto;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TimelineSuccessCode implements SuccessCode {

    GET_TIMELINE(HttpStatus.OK, "타임라인이 열람되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
