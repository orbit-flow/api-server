package com.backend.orbitflow.domain.like.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LikeSuccessCode implements SuccessCode {

    LIKE_TOGGLE(HttpStatus.OK, "좋아요 토글이 완료되었습니다."),
    GET_LIKER_LIST(HttpStatus.OK, "좋아요 리스트가 열람되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
