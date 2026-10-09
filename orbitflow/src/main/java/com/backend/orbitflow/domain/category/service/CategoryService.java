package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.category.dto.response.CategoryPermissionResponse;
import com.backend.orbitflow.domain.category.dto.response.CategoryResponse;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.enums.Visibility;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;

public interface CategoryService {

    Category getActiveCategory(Long categoryId);
    CategoryResponse createPersonalCategory(User user, String name, String color, Visibility visibility);
    CategoryResponse createTeamCategory(Team team, User actor, String name, String color, Visibility visibility);
    List<CategoryResponse> getMyCategories(User me);
    List<CategoryResponse> getUserCategories(User viewer, User owner);
    List<CategoryResponse> getTeamCategories(User viewer, Team team);
    CategoryResponse getCategory(User viewer, Long categoryId);
    CategoryResponse updateCategory(User actor, Long categoryId, String name, String color, Visibility visibility);
    void deleteCategory(User actor, Long categoryId, Long moveToCategoryId);
    CategoryPermissionResponse getPermissions(User actor, Long categoryId);
    CategoryPermissionResponse updatePermissions(User actor, Long categoryId, List<Long> roleIds, List<User> members);
}
