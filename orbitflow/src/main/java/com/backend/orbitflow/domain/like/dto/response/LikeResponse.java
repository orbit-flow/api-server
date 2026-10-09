package com.backend.orbitflow.domain.like.dto.response;

// 토글 결과 : liked = 토글 후 요청자의 좋아요 상태
public record LikeResponse(
        Long postId,
        boolean liked,
        long likeCount
) {
}
