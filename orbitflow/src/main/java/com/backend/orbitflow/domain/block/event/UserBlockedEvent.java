package com.backend.orbitflow.domain.block.event;

// 차단 생성 (커밋 후 채팅 도메인이 두 사용자 사이 1:1 대화의 실시간 구독을 양쪽 모두 해제)
public record UserBlockedEvent(Long blockerId, Long blockeeId) {
}
