package com.backend.orbitflow.domain.post.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostSuccessCode implements SuccessCode {

    POST_CREATE(HttpStatus.CREATED, "게시글이 작성되었습니다."),
    GET_POST_LIST(HttpStatus.OK, "게시글 리스트가 열람되었습니다."),
    GET_POST_INFO(HttpStatus.OK, "게시글이 열람되었습니다."),
    POST_UPDATE(HttpStatus.OK, "게시글이 수정되었습니다."),
    POST_DELETE(HttpStatus.OK, "게시글이 삭제되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
