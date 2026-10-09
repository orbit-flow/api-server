package com.backend.orbitflow.domain.notification.event;

// 팀을 떠난 구성원의 미완료 투두 상속 (상속받은 구성원별로 묶어 1건)
public record TodosInheritedEvent(Long teamId, Long leaverId, Long heirId, int count) {
}
