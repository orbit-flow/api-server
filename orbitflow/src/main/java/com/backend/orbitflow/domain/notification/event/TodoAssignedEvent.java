package com.backend.orbitflow.domain.notification.event;

// 팀 투두 담당자 지정 (생성 시 지정 포함, 본인이 본인에게 지정한 경우는 제외)
public record TodoAssignedEvent(Long todoId, Long assigneeId, Long actorId) {
}
