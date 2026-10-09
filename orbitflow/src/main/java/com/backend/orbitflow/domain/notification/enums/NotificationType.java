package com.backend.orbitflow.domain.notification.enums;

public enum NotificationType {
    REMINDER,
    SOCIAL,               // 팔로우, 팔로우 요청, 팔로우 요청 승인
    TODO_COMPLETED,
    TODO_ASSIGNED,        // 팀 투두 배정, 팀을 떠난 구성원의 투두 상속
    NEWPOST,
    NEWCOMMENT,
    LIKE,
    TEAM_JOINED,
    TEAM_LEFT,
    TEAM_ROLE_CHANGED,
    CATEGORY_VISIBILITY_CHANGED,
    POINT_EARNED,
    POINT_REVOKED,
    ITEM_PURCHASED
}
