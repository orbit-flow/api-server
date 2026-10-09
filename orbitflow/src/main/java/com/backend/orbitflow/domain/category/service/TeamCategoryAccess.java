package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.service.TeamPermissionSnapshot;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 한 팀의 구성원 권한 + 팀 카테고리 열람 허용 대상을 한 번에 조회한 결과 (팀 단위 쿼리 3회, 이후 판정은 메모리)
 *
 * <p>팀 카테고리 공개 범위 규칙 (CategoryAuthorityService.filterViewableTeam과 동일)
 * <ul>
 *   <li>PUBLIC · FOLLOWER : 모든 구성원</li>
 *   <li>PRIVATE : MANAGE_CATEGORIES 권한 보유자, 또는 열람 허용으로 지정된 역할·구성원</li>
 * </ul>
 */
public record TeamCategoryAccess(
        TeamPermissionSnapshot permissions,
        Map<Long, Set<Long>> memberIdsByCategoryId,
        Map<Long, Set<Long>> roleIdsByCategoryId
) {

    public List<TeamMember> members() {
        return permissions.members();
    }

    public boolean canView(Category category, TeamMember member) {
        return switch (category.getVisibility()) {
            case PUBLIC, FOLLOWER -> true;
            case PRIVATE -> permissions.has(member, TeamPermission.MANAGE_CATEGORIES)
                    || memberIdsByCategoryId.getOrDefault(category.getId(), Set.of()).contains(member.getId())
                    || !Collections.disjoint(roleIdsByCategoryId.getOrDefault(category.getId(), Set.of()), permissions.roleIds(member));
        };
    }

    // 카테고리를 볼 수 있는 구성원의 사용자 id
    public Set<Long> viewerUserIds(Category category) {
        return members().stream()
                .filter(member -> canView(category, member))
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());
    }
}
