package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.enums.Visibility;

import java.util.List;
import java.util.Set;

/**
 * 한 사용자가 팀 카테고리를 볼 수 있는 범위 (사용자 단위 쿼리 3회)
 *
 * <p>여러 팀의 카테고리를 섞어서 판정해야 하는 목록(타임라인 등)에서 카테고리마다 권한을 조회하지 않도록 사용
 * <ul>
 *   <li>memberTeamIds : 소속 팀 (FOLLOWER 범위)</li>
 *   <li>managerTeamIds : MANAGE_CATEGORIES 권한이 있는 팀 (소유자 포함, 모든 PRIVATE 카테고리 열람)</li>
 *   <li>allowedCategoryIds : 본인 또는 본인 역할에 열람이 허용된 PRIVATE 카테고리</li>
 * </ul>
 */
public record ViewerTeamScope(
        Set<Long> memberTeamIds,
        Set<Long> managerTeamIds,
        Set<Long> allowedCategoryIds
) {

    // 네이티브 쿼리의 IN 조건용 : 빈 목록이면 어떤 id와도 일치하지 않는 값을 넣음
    private static final List<Long> NONE = List.of(-1L);

    public boolean canView(Category category) {
        Long teamId = category.getTeam().getId();
        Visibility visibility = category.getVisibility();
        return visibility == Visibility.PUBLIC
                || (visibility == Visibility.FOLLOWER && memberTeamIds.contains(teamId))
                || (visibility == Visibility.PRIVATE
                    && (managerTeamIds.contains(teamId) || allowedCategoryIds.contains(category.getId())));
    }

    public List<Long> memberTeamIdsOrNone() {
        return orNone(memberTeamIds);
    }

    public List<Long> managerTeamIdsOrNone() {
        return orNone(managerTeamIds);
    }

    public List<Long> allowedCategoryIdsOrNone() {
        return orNone(allowedCategoryIds);
    }

    private static List<Long> orNone(Set<Long> ids) {
        return ids.isEmpty() ? NONE : List.copyOf(ids);
    }
}
