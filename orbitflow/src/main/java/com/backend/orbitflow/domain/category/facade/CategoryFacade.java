package com.backend.orbitflow.domain.category.facade;

import com.backend.orbitflow.domain.category.dto.request.CategoryPermissionRequest;
import com.backend.orbitflow.domain.category.dto.request.CategoryRequest;
import com.backend.orbitflow.domain.category.dto.response.CategoryPermissionResponse;
import com.backend.orbitflow.domain.category.dto.response.CategoryResponse;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.enums.Visibility;
import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.category.service.CategoryAuthorityService;
import com.backend.orbitflow.domain.category.service.CategoryService;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.team.service.TeamRoleService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.todo.service.TodoTransferService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.backend.orbitflow.global.common.dto.response.PageResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CategoryFacade {

    private final CategoryService categoryService;
    private final CategoryAuthorityService categoryAuthorityService;
    private final UserService userService;
    private final TeamService teamService;
    private final TeamAuthorityService teamAuthorityService;
    private final TeamRoleService teamRoleService;
    private final TodoTransferService todoTransferService;
    private final BlockService blockService;
    private final FollowService followService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CategoryResponse createPersonalCategory(AuthUser authUser, CategoryRequest request) {
        return categoryService.createPersonalCategory(
                userService.getActiveByUuid(authUser.getUuid()),
                request.name(),
                request.color(),
                request.visibility()
        );
    }

    @Transactional
    public CategoryResponse createTeamCategory(AuthUser authUser, String teamUuid, CategoryRequest request) {
        Team team = teamService.getActiveTeam(teamUuid);
        User me = userService.getActiveByUuid(authUser.getUuid());
        categoryAuthorityService.checkManage(teamMask(team, me));
        return categoryService.createTeamCategory(
                team,
                request.name(),
                request.color(),
                request.visibility()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getMyCategories(AuthUser authUser, int page, int size) {
        return PageResponse.from(categoryService.getMyCategories(userService.getActiveByUuid(authUser.getUuid()), page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getUserCategories(AuthUser authUser, String userUuid, int page, int size) {
        return PageResponse.from(categoryService.getUserCategories(
                userService.getActiveByUuid(authUser.getUuid()),
                userService.getByUuid(userUuid),
                page, size
        ));
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getTeamCategories(AuthUser authUser, String teamUuid, int page, int size) {
        return PageResponse.from(categoryService.getTeamCategories(
                userService.getActiveByUuid(authUser.getUuid()),
                teamService.getActiveTeam(teamUuid),
                page, size
        ));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(AuthUser authUser, Long categoryId) {
        User me = userService.getActiveByUuid(authUser.getUuid());
        Category category = categoryService.getActiveCategory(categoryId);
        categoryAuthorityService.checkView(canView(category, me));
        return categoryService.getCategory(category);
    }

    // 팀 카테고리 공개 범위 변경 시 변경 전 또는 변경 후에 조회 권한이 있는 팀원에게 알림 (변경한 본인 제외)
    // 변경 전·후 중 한쪽은 반드시 PRIVATE가 아니므로(PUBLIC·FOLLOWER는 모든 팀원 조회 가능) 대상은 모든 팀원
    @Transactional
    public CategoryResponse updateCategory(AuthUser authUser, Long categoryId, CategoryRequest request) {
        User me = userService.getActiveByUuid(authUser.getUuid());
        Category category = categoryService.getActiveCategory(categoryId);
        categoryAuthorityService.checkEdit(category, me, teamMask(category, me));
        Visibility before = category.getVisibility();
        CategoryResponse response = categoryService.updateCategory(category, request.name(), request.color(), request.visibility());
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
        User me = userService.getActiveByUuid(authUser.getUuid());
        Category category = categoryService.getActiveCategory(categoryId);
        categoryAuthorityService.checkEdit(category, me, teamMask(category, me));
        Category moveTo = categoryService.getMoveTarget(category, moveToCategoryId);
        todoTransferService.moveAllToCategory(category, moveTo);
        categoryService.deleteCategory(category);
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryPermissionResponse> getPermissions(AuthUser authUser, Long categoryId, int page, int size) {
        User me = userService.getActiveByUuid(authUser.getUuid());
        Category category = categoryService.getActiveCategory(categoryId);
        categoryService.requireTeamCategory(category);
        categoryAuthorityService.checkEdit(category, me, teamMask(category, me));
        return PageResponse.from(categoryService.getPermissions(category, page, size));
    }

    @Transactional
    public void updatePermissions(AuthUser authUser, Long categoryId, CategoryPermissionRequest request) {
        User me = userService.getActiveByUuid(authUser.getUuid());
        List<User> members = userService.getAllByUuids(request.memberUuids());
        Category category = categoryService.getActiveCategory(categoryId);
        categoryService.requireTeamCategory(category);
        categoryAuthorityService.checkEdit(category, me, teamMask(category, me));
        Team team = category.getTeam();
        Set<Long> roleIds = new HashSet<>(request.roleIds());
        List<TeamRole> roles = teamRoleService.findRoles(team, roleIds);
        List<TeamMember> teamMembers = teamAuthorityService.findMembers(team, members);
        categoryService.updatePermissions(category, roleIds, roles, members, teamMembers);
    }

    // 팀에서의 권한 mask (팀 구성원이 아니면 null)
    private Integer teamMask(Team team, User user) {
        return teamAuthorityService.findMember(team, user)
                .map(member -> teamAuthorityService.getPermissionMask(team, member))
                .orElse(null);
    }

    // 팀 카테고리일 때만 조회 (개인 카테고리는 팀 권한을 보지 않으므로 null)
    private Integer teamMask(Category category, User user) {
        return category.isTeamCategory() ? teamMask(category.getTeam(), user) : null;
    }

    // 카테고리 열람 가능 여부 : 소유 도메인(팀·차단·팔로우)의 정보를 조회해 정책에 전달
    private boolean canView(Category category, User viewer) {
        if (category.isTeamCategory()) {
            Team team = category.getTeam();
            TeamMember member = teamAuthorityService.findMember(team, viewer).orElse(null);
            Integer mask = member == null ? null : teamAuthorityService.getPermissionMask(team, member);
            return categoryAuthorityService.canViewTeam(category, member, mask);
        }
        User owner = category.getUser();
        // 본인 카테고리는 관계를 조회할 필요가 없고, 차단 관계면 팔로우 상태는 판정에 쓰이지 않음
        boolean self = owner.getId().equals(viewer.getId());
        boolean blocked = !self && blockService.isBlocked(owner, viewer);
        FollowState followState = self || blocked ? null : followService.getFollowState(viewer, owner);
        return categoryAuthorityService.canViewPersonal(category, viewer, blocked, followState);
    }
}
