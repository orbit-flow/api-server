package com.backend.orbitflow.domain.block.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BlockSuccessCode implements SuccessCode {

    GET_BLOCK_LIST(HttpStatus.OK, "리스트가 열람되었습니다."),
    BLOCK_SUCCESS(HttpStatus.OK, "사용자 차단 토글이 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
