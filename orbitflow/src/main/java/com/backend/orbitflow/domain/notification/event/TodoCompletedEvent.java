package com.backend.orbitflow.domain.notification.event;

// 투두 완료 (완료 취소는 제외)
public record TodoCompletedEvent(Long todoId, Long completerId) {
}
