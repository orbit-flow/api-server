package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.category.dto.response.CategoryPermissionResponse;
import com.backend.orbitflow.domain.category.dto.response.CategoryResponse;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.enums.Visibility;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;
import org.springframework.data.domain.Page;

public interface CategoryService {

    Category getActiveCategory(Long categoryId);
    CategoryResponse createPersonalCategory(User user, String name, String color, Visibility visibility);
    CategoryResponse createTeamCategory(Team team, User actor, String name, String color, Visibility visibility);
    Page<CategoryResponse> getMyCategories(User me, int page, int size);
    Page<CategoryResponse> getUserCategories(User viewer, User owner, int page, int size);
    Page<CategoryResponse> getTeamCategories(User viewer, Team team, int page, int size);
    CategoryResponse getCategory(User viewer, Long categoryId);
    CategoryResponse updateCategory(User actor, Long categoryId, String name, String color, Visibility visibility);
    void deleteCategory(User actor, Long categoryId, Long moveToCategoryId);
    void deleteAllMemberPermissionsByTeam(Team team);
    Page<CategoryPermissionResponse> getPermissions(User actor, Long categoryId, int page, int size);
    void updatePermissions(User actor, Long categoryId, List<Long> roleIds, List<User> members);
}
