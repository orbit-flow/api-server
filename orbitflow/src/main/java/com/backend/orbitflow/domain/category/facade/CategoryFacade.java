package com.backend.orbitflow.domain.category.facade;

import com.backend.orbitflow.domain.category.dto.request.CategoryPermissionRequest;
import com.backend.orbitflow.domain.category.dto.request.CategoryRequest;
import com.backend.orbitflow.domain.category.dto.response.CategoryPermissionResponse;
import com.backend.orbitflow.domain.category.dto.response.CategoryResponse;
import com.backend.orbitflow.domain.category.service.CategoryService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryFacade {

    private final CategoryService categoryService;
    private final UserService userService;
    private final TeamService teamService;

    @Transactional
    public CategoryResponse createPersonalCategory(AuthUser authUser, CategoryRequest request) {
        return categoryService.createPersonalCategory(
                userService.getByUuid(authUser.getUuid()),
                request.name(),
                request.color(),
                request.visibility()
        );
    }

    @Transactional
    public CategoryResponse createTeamCategory(AuthUser authUser, String teamUuid, CategoryRequest request) {
        return categoryService.createTeamCategory(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                request.name(),
                request.color(),
                request.visibility()
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getMyCategories(AuthUser authUser) {
        return categoryService.getMyCategories(userService.getByUuid(authUser.getUuid()));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getUserCategories(AuthUser authUser, String userUuid) {
        return categoryService.getUserCategories(
                userService.getByUuid(authUser.getUuid()),
                userService.getByUuid(userUuid)
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getTeamCategories(AuthUser authUser, String teamUuid) {
        return categoryService.getTeamCategories(
                userService.getByUuid(authUser.getUuid()),
                teamService.getActiveTeam(teamUuid)
        );
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(AuthUser authUser, Long categoryId) {
        return categoryService.getCategory(userService.getByUuid(authUser.getUuid()), categoryId);
    }

    @Transactional
    public CategoryResponse updateCategory(AuthUser authUser, Long categoryId, CategoryRequest request) {
        return categoryService.updateCategory(
                userService.getByUuid(authUser.getUuid()),
                categoryId,
                request.name(),
                request.color(),
                request.visibility()
        );
    }

    @Transactional
    public void deleteCategory(AuthUser authUser, Long categoryId, Long moveToCategoryId) {
        categoryService.deleteCategory(userService.getByUuid(authUser.getUuid()), categoryId, moveToCategoryId);
    }

    @Transactional(readOnly = true)
    public CategoryPermissionResponse getPermissions(AuthUser authUser, Long categoryId) {
        return categoryService.getPermissions(userService.getByUuid(authUser.getUuid()), categoryId);
    }

    @Transactional
    public CategoryPermissionResponse updatePermissions(AuthUser authUser, Long categoryId, CategoryPermissionRequest request) {
        return categoryService.updatePermissions(
                userService.getByUuid(authUser.getUuid()),
                categoryId,
                request.roleIds(),
                request.memberUuids().stream()
                        .map(userService::getByUuid)
                        .toList()
        );
    }
}
