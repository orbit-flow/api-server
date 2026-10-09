package com.backend.orbitflow.domain.notification.event;

// 팀 가입 (초대 수락)
public record TeamJoinedEvent(Long teamId, Long userId) {
}
