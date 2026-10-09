package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;

public interface CategoryAuthorityService {

    boolean canView(Category category, User viewer);
    TeamCategoryAccess teamAccess(Team team);
    ViewerTeamScope viewerScope(User viewer);
    void checkView(Category category, User viewer);
    void checkEdit(Category category, User actor);
    List<Category> filterViewablePersonal(User owner, User viewer, List<Category> categories);
    List<Category> filterViewableTeam(Team team, User viewer, List<Category> categories);
}
