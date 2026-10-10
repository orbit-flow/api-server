package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CategoryAuthorityService {

    boolean canView(Category category, User viewer);
    void checkView(Category category, User viewer);
    void checkEdit(Category category, User actor);
    List<Category> filterViewablePersonal(User owner, User viewer, List<Category> categories);
    List<Category> filterViewableTeam(Team team, User viewer, List<Category> categories);

    // 알림 대상 조회용
    Set<Long> findViewerUserIds(Category teamCategory);
    List<Map<Long, String>> findNotifiableViewers(Category category, User followee);
}
