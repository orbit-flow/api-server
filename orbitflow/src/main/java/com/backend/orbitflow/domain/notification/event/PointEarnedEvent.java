package com.backend.orbitflow.domain.notification.event;

// 포인트 적립 (출석 등)
public record PointEarnedEvent(Long userId, int amount, int balanceAfter) {
}
