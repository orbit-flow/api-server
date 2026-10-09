package com.backend.orbitflow.domain.category.enums;

// 개인 카테고리 : PUBLIC 전체 공개 / FOLLOWER 승인된 팔로워 / PRIVATE 나만 보기
// 팀 카테고리   : PUBLIC 전체 공개 / FOLLOWER 모든 팀원 / PRIVATE category_permissions에 지정된 역할·팀원
public enum Visibility {
    PUBLIC, FOLLOWER, PRIVATE;

    // 두 공개 범위 중 더 제한적인 쪽
    public Visibility mostRestrictive(Visibility other) {
        return this.ordinal() >= other.ordinal() ? this : other;
    }
}
