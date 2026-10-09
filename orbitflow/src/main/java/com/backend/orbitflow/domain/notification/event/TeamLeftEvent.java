package com.backend.orbitflow.domain.notification.event;

// 팀 탈퇴 또는 추방
public record TeamLeftEvent(Long teamId, Long userId, boolean kicked) {
}
