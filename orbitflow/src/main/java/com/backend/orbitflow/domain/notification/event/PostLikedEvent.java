package com.backend.orbitflow.domain.notification.event;

// 게시글 좋아요
public record PostLikedEvent(Long postId, Long likerId) {
}
