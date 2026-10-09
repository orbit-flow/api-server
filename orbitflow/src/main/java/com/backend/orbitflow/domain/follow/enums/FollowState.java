package com.backend.orbitflow.domain.follow.enums;

public enum FollowState {
    PENDING, ACCEPTED,
    // DB에 저장하지 않는 응답 전용 상태 : 요청자가 대상 사용자를 팔로우하지 않은 경우 (follows row 없음)
    NOT_FOLLOW
}
