package com.backend.orbitflow.domain.notification.event;

// 팔로우(공개계정) 또는 팔로우 요청(비밀계정) 생성
public record FollowCreatedEvent(Long followId) {
}
