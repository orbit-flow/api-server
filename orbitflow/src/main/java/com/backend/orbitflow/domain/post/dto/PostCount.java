package com.backend.orbitflow.domain.post.dto;

// 게시글별 좋아요·댓글 수 집계 결과
public record PostCount(
        Long postId,
        Long count
) {
}
