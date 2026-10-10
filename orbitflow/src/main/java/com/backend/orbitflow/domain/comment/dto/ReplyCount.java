package com.backend.orbitflow.domain.comment.dto;

// 최상위 댓글별 대댓글 수
public record ReplyCount(Long parentId, long count) {
}
