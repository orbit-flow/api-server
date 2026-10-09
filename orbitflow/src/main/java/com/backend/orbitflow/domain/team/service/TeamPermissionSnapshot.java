package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.enums.TeamPermission;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 팀 전체 구성원의 권한을 한 번에 계산한 결과 (구성원 수와 무관하게 쿼리 2회)
 *
 * <p>구성원마다 권한을 조회하는 대신 여러 구성원을 다루는 작업(알림 대상 선정, 투두 상속, 공개 범위 판정)에서 사용
 * <p>소유자는 항상 전체 권한, 그 외에는 보유 역할의 권한을 OR 합산 (TeamAuthorityService.getPermissionMask와 같은 규칙)
 */
public record TeamPermissionSnapshot(
        Team team,
        // 사용자 포함 조회, 가입 순
        List<TeamMember> members,
        Map<Long, Integer> maskByMemberId,
        Map<Long, Set<Long>> roleIdsByMemberId
) {

    public int mask(TeamMember member) {
        return maskByMemberId.getOrDefault(member.getId(), 0);
    }

    public boolean has(TeamMember member, TeamPermission permission) {
        return permission.isGranted(mask(member));
    }

    public Set<Long> roleIds(TeamMember member) {
        return roleIdsByMemberId.getOrDefault(member.getId(), Set.of());
    }
}
