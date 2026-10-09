package com.backend.orbitflow.domain.comment.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentSuccessCode implements SuccessCode {

    COMMENT_CREATE(HttpStatus.CREATED, "댓글이 작성되었습니다."),
    GET_COMMENT_LIST(HttpStatus.OK, "댓글 리스트가 열람되었습니다."),
    COMMENT_UPDATE(HttpStatus.OK, "댓글이 수정되었습니다."),
    COMMENT_DELETE(HttpStatus.OK, "댓글이 삭제되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
