package com.backend.orbitflow.domain.notification.event;

// 포인트 적립 (reason : 출석, 충전 등 적립 사유)
public record PointEarnedEvent(Long userId, String reason, int amount, int balanceAfter) {
}
