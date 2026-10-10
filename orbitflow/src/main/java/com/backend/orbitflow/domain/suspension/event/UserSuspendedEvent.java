package com.backend.orbitflow.domain.suspension.event;

// 계정 정지 (커밋 후 채팅 도메인이 사용자의 모든 실시간 구독 해제)
public record UserSuspendedEvent(String userUuid) {
}
