package com.backend.orbitflow.domain.notification.event;

// 무효·부정 출석 포인트 회수 (amount : 회수한 포인트, 양수)
public record PointRevokedEvent(Long userId, int amount, int balanceAfter) {
}
