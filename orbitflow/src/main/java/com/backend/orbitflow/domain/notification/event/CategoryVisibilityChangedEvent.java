package com.backend.orbitflow.domain.notification.event;

// 팀 카테고리 공개 범위 변경 (viewerIdsBefore : 변경 전 조회 권한이 있던 팀원 사용자 id)
public record CategoryVisibilityChangedEvent(Long categoryId, Long actorId, java.util.Set<Long> viewerIdsBefore) {
}
