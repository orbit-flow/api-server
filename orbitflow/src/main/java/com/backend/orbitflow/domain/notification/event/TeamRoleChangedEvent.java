package com.backend.orbitflow.domain.notification.event;

// 구성원 역할 변경
public record TeamRoleChangedEvent(Long teamId, Long userId) {
}
