package com.backend.orbitflow.domain.team.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// team_roles.permissions_mask 비트 정의
// 구성원에게 복수 역할이 있으면 각 역할의 mask를 OR 합산하여 판정, 팀 소유자(owner)는 항상 전체 권한
// 팀 관리자 = MANAGE_TEAM 권한이 포함된 역할을 가진 구성원
@Getter
@RequiredArgsConstructor
public enum TeamPermission {

    MANAGE_TEAM(1),         // 팀 정보 수정
    MANAGE_MEMBERS(1 << 1), // 구성원 초대·추방, 초대 취소
    MANAGE_ROLES(1 << 2),   // 역할 생성·수정·부여
    MANAGE_CATEGORIES(1 << 3),
    MANAGE_TODOS(1 << 4),
    ASSIGN_TODOS(1 << 5);

    private final int bit;

    public boolean isGranted(int mask) {
        return (mask & bit) != 0;
    }

    public static int all() {
        int mask = 0;
        for (TeamPermission permission : values()) {
            mask |= permission.bit;
        }
        return mask;
    }
}
