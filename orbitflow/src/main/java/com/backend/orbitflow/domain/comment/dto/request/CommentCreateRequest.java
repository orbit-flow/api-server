package com.backend.orbitflow.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용은 비어있을 수 없습니다.")
        @Size(max = 1000, message = "댓글 내용은 1000자 이내여야 합니다.")
        String content,

        // 대댓글 작성 시 부모 댓글 id
        Long parentCommentId
) {
}
