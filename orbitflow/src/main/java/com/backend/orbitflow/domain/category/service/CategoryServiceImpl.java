package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.category.dto.response.CategoryPermissionResponse;
import com.backend.orbitflow.domain.category.dto.response.CategoryResponse;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.entity.CategoryPermission;
import com.backend.orbitflow.domain.category.enums.Visibility;
import com.backend.orbitflow.domain.category.error.CategoryErrorCode;
import com.backend.orbitflow.domain.category.repository.CategoryPermissionRepository;
import com.backend.orbitflow.domain.category.repository.CategoryRepository;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.team.repository.TeamRoleRepository;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.todo.repository.TodoRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryPermissionRepository categoryPermissionRepository;
    private final CategoryAuthorityService categoryAuthorityService;
    private final TeamAuthorityService teamAuthorityService;
    private final TeamRoleRepository teamRoleRepository;
    private final TodoRepository todoRepository;

    public CategoryResponse createPersonalCategory(User user, String name, String color, Visibility visibility) {
        return CategoryResponse.from(categoryRepository.save(Category.personal(user, name, color, visibility)));
    }

    public CategoryResponse createTeamCategory(Team team, User actor, String name, String color, Visibility visibility) {
        teamAuthorityService.checkPermission(team, actor, TeamPermission.MANAGE_CATEGORIES);
        return CategoryResponse.from(categoryRepository.save(Category.team(team, name, color, visibility)));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getMyCategories(User me) {
        return toResponses(categoryRepository.findAllByUserOrderByCreatedAtAsc(me));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getUserCategories(User viewer, User owner) {
        return toResponses(categoryAuthorityService.filterViewablePersonal(
                owner, viewer, categoryRepository.findAllByUserOrderByCreatedAtAsc(owner)
        ));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getTeamCategories(User viewer, Team team) {
        return toResponses(categoryAuthorityService.filterViewableTeam(
                team, viewer, categoryRepository.findAllByTeamOrderByCreatedAtAsc(team)
        ));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(User viewer, Long categoryId) {
        Category category = getActiveCategory(categoryId);
        categoryAuthorityService.checkView(category, viewer);
        return CategoryResponse.from(category);
    }

    // 공개 범위 변경은 저장 즉시 적용 (소속 투두는 카테고리 공개 범위를 따르므로 함께 적용됨)
    public CategoryResponse updateCategory(User actor, Long categoryId, String name, String color, Visibility visibility) {
        Category category = getActiveCategory(categoryId);
        categoryAuthorityService.checkEdit(category, actor);
        category.updateCategory(name, color, visibility);
        // TODO: 알림 도메인 구현 후 팀 카테고리 공개 범위 변경 시 변경 전·후 조회 권한이 있는 팀원에게 CATEGORY_VISIBILITY_CHANGED 알림
        return CategoryResponse.from(category);
    }

    // 소속 투두는 같은 소유자의 다른 카테고리로 이동, 같은 소유자의 다른 카테고리가 없으면 삭제 불가
    public void deleteCategory(User actor, Long categoryId, Long moveToCategoryId) {
        Category category = getActiveCategory(categoryId);
        categoryAuthorityService.checkEdit(category, actor);
        if (categoryId.equals(moveToCategoryId)) {
            throw new CommonException(CategoryErrorCode.INVALID_MOVE_TARGET);
        }
        Category moveTo = categoryRepository.findWithOwnerById(moveToCategoryId)
                .filter(category::isSameOwner)
                .orElseThrow(() -> new CommonException(CategoryErrorCode.INVALID_MOVE_TARGET));
        todoRepository.moveAllToCategory(category, moveTo);
        categoryPermissionRepository.deleteAllByCategory(category);
        categoryRepository.deleteById(category.getId());
    }

    @Transactional(readOnly = true)
    public CategoryPermissionResponse getPermissions(User actor, Long categoryId) {
        Category category = getTeamCategoryForManage(actor, categoryId);
        return CategoryPermissionResponse.of(category.getId(), categoryPermissionRepository.findAllWithTargetByCategory(category));
    }

    // PRIVATE 팀 카테고리의 열람 허용 대상을 요청 목록으로 교체
    public CategoryPermissionResponse updatePermissions(User actor, Long categoryId, List<Long> roleIds, List<User> members) {
        Category category = getTeamCategoryForManage(actor, categoryId);
        Team team = category.getTeam();

        Set<Long> roleIdSet = new HashSet<>(roleIds);
        List<TeamRole> roles = teamRoleRepository.findAllByTeamAndIdIn(team, roleIdSet);
        if (roles.size() != roleIdSet.size()) {
            throw new CommonException(TeamErrorCode.ROLE_NOT_FOUND);
        }
        List<TeamMember> teamMembers = members.stream()
                .distinct()
                .map(user -> teamAuthorityService.findMember(team, user).orElseThrow(
                        () -> new CommonException(TeamErrorCode.MEMBER_NOT_FOUND)
                ))
                .toList();

        categoryPermissionRepository.deleteAllByCategory(category);
        List<CategoryPermission> permissions = new ArrayList<>();
        roles.forEach(role -> permissions.add(CategoryPermission.ofRole(category, role)));
        teamMembers.forEach(member -> permissions.add(CategoryPermission.ofMember(category, member)));
        categoryPermissionRepository.saveAll(permissions);
        return CategoryPermissionResponse.of(category.getId(), categoryPermissionRepository.findAllWithTargetByCategory(category));
    }

    // 소유자가 탈퇴했거나 팀이 삭제된 카테고리는 존재하지 않는 것으로 처리
    @Transactional(readOnly = true)
    public Category getActiveCategory(Long categoryId) {
        Category category = categoryRepository.findWithOwnerById(categoryId).orElseThrow(
                () -> new CommonException(CategoryErrorCode.CATEGORY_NOT_FOUND)
        );
        boolean ownerDeleted = category.isTeamCategory()
                ? category.getTeam().getDeletedAt() != null
                : category.getUser().getDeletedAt() != null;
        if (ownerDeleted) {
            throw new CommonException(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private Category getTeamCategoryForManage(User actor, Long categoryId) {
        Category category = getActiveCategory(categoryId);
        if (!category.isTeamCategory()) {
            throw new CommonException(CategoryErrorCode.NOT_TEAM_CATEGORY);
        }
        categoryAuthorityService.checkEdit(category, actor);
        return category;
    }

    private List<CategoryResponse> toResponses(List<Category> categories) {
        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
