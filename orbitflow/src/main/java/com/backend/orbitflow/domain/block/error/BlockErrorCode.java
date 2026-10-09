package com.backend.orbitflow.domain.block.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BlockErrorCode implements ErrorCode {

    SELF_BLOCK(HttpStatus.BAD_REQUEST,
            "자기 자신을 차단할 수 없습니다.",
            "https://orbitflow.com/errors/self-block",
            "Self Block");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
