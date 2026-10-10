package com.backend.orbitflow.domain.category.facade;

import com.backend.orbitflow.domain.category.dto.request.CategoryPermissionRequest;
import com.backend.orbitflow.domain.category.dto.request.CategoryRequest;
import com.backend.orbitflow.domain.category.dto.response.CategoryPermissionResponse;
import com.backend.orbitflow.domain.category.dto.response.CategoryResponse;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.enums.Visibility;
import com.backend.orbitflow.domain.category.service.CategoryService;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.backend.orbitflow.global.common.dto.response.PageResponse;

@Component
@RequiredArgsConstructor
public class CategoryFacade {

    private final CategoryService categoryService;
    private final UserService userService;
    private final TeamService teamService;
    private final TeamAuthorityService teamAuthorityService;
    private final ApplicationEventPublisher eventPublisher;

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
    public PageResponse<CategoryResponse> getMyCategories(AuthUser authUser, int page, int size) {
        return PageResponse.from(categoryService.getMyCategories(userService.getByUuid(authUser.getUuid()), page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getUserCategories(AuthUser authUser, String userUuid, int page, int size) {
        return PageResponse.from(categoryService.getUserCategories(
                userService.getByUuid(authUser.getUuid()),
                userService.getByUuid(userUuid),
                page, size
        ));
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getTeamCategories(AuthUser authUser, String teamUuid, int page, int size) {
        return PageResponse.from(categoryService.getTeamCategories(
                userService.getByUuid(authUser.getUuid()),
                teamService.getActiveTeam(teamUuid),
                page, size
        ));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(AuthUser authUser, Long categoryId) {
        return categoryService.getCategory(userService.getByUuid(authUser.getUuid()), categoryId);
    }

    // 팀 카테고리 공개 범위 변경 시 변경 전 또는 변경 후에 조회 권한이 있는 팀원에게 알림 (변경한 본인 제외)
    // 변경 전·후 중 한쪽은 반드시 PRIVATE가 아니므로(PUBLIC·FOLLOWER는 모든 팀원 조회 가능) 대상은 모든 팀원
    @Transactional
    public CategoryResponse updateCategory(AuthUser authUser, Long categoryId, CategoryRequest request) {
        User me = userService.getByUuid(authUser.getUuid());
        Category category = categoryService.getActiveCategory(categoryId);
        Visibility before = category.getVisibility();
        CategoryResponse response = categoryService.updateCategory(me, categoryId, request.name(), request.color(), request.visibility());
        // 요청의 visibility가 null이면 기본값으로 저장되므로 저장된 값(응답)으로 비교
        Visibility after = response.visibility();
        if (category.isTeamCategory() && before != after) {
            Team team = category.getTeam();
            eventPublisher.publishEvent(NotificationRequest.toAll(teamAuthorityService.findMemberUsers(team),
                    NotificationType.CATEGORY_VISIBILITY_CHANGED, me, category.getId(), team.getUuid(),
                    "'" + team.getName() + "' 팀의 '" + category.getName() + "' 카테고리 공개 범위가 "
                            + after + "(으)로 변경되었습니다."));
        }
        return response;
    }

    @Transactional
    public void deleteCategory(AuthUser authUser, Long categoryId, Long moveToCategoryId) {
        categoryService.deleteCategory(userService.getByUuid(authUser.getUuid()), categoryId, moveToCategoryId);
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryPermissionResponse> getPermissions(AuthUser authUser, Long categoryId, int page, int size) {
        return PageResponse.from(categoryService.getPermissions(userService.getByUuid(authUser.getUuid()), categoryId, page, size));
    }

    @Transactional
    public void updatePermissions(AuthUser authUser, Long categoryId, CategoryPermissionRequest request) {
        categoryService.updatePermissions(
                userService.getByUuid(authUser.getUuid()),
                categoryId,
                request.roleIds(),
                userService.getAllByUuids(request.memberUuids())
        );
    }
}
