package com.backend.orbitflow.domain.comment.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 댓글입니다.",
            "https://orbitflow.com/errors/comment-not-found",
            "Comment Not Found"),
    INVALID_PARENT_COMMENT(HttpStatus.BAD_REQUEST,
            "대댓글은 같은 게시글의 댓글에 한 단계까지만 작성할 수 있습니다.",
            "https://orbitflow.com/errors/invalid-parent-comment",
            "Invalid Parent Comment"),
    COMMENT_BLOCKED(HttpStatus.FORBIDDEN,
            "차단 관계인 사용자의 댓글에는 대댓글을 작성할 수 없습니다.",
            "https://orbitflow.com/errors/comment-blocked",
            "Comment Blocked"),
    NOT_COMMENT_AUTHOR(HttpStatus.FORBIDDEN,
            "댓글 작성자만 수행할 수 있습니다.",
            "https://orbitflow.com/errors/not-comment-author",
            "Not Comment Author"),
    COMMENT_DELETE_DENIED(HttpStatus.FORBIDDEN,
            "댓글 작성자 또는 게시글 작성자만 삭제할 수 있습니다.",
            "https://orbitflow.com/errors/comment-delete-denied",
            "Comment Delete Denied");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
