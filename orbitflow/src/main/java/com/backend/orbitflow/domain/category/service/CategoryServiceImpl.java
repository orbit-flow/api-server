package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.category.dto.response.CategoryPermissionResponse;
import com.backend.orbitflow.domain.category.dto.response.CategoryResponse;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.enums.Visibility;
import com.backend.orbitflow.domain.category.error.CategoryErrorCode;
import com.backend.orbitflow.domain.category.repository.CategoryPermissionRepository;
import com.backend.orbitflow.domain.category.repository.CategoryRepository;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.UserStatus;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.backend.orbitflow.global.util.JdbcBulkInserter;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final JdbcBulkInserter jdbcBulkInserter;
    private final CategoryRepository categoryRepository;
    private final CategoryPermissionRepository categoryPermissionRepository;

    public CategoryResponse createPersonalCategory(User user, String name, String color, Visibility visibility) {
        return CategoryResponse.from(categoryRepository.save(Category.personal(user, name, color, visibility)));
    }

    public CategoryResponse createTeamCategory(Team team, String name, String color, Visibility visibility) {
        return CategoryResponse.from(categoryRepository.save(Category.team(team, name, color, visibility)));
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getMyCategories(User me, int page, int size) {
        Pageable pageable = toPageable(page, size);
        return categoryRepository.findAllByUserOrderByCreatedAtAscIdAsc(me, pageable).map(CategoryResponse::from);
    }

    @Transactional(readOnly = true)
    // 열람 판정은 조회 쿼리에서 (조회 후 걸러내지 않으므로 페이지 크기가 정확함)
    public Page<CategoryResponse> getUserCategories(User viewer, User owner, int page, int size) {
        return categoryRepository.findViewablePersonal(owner.getId(), viewer.getId(), toPageable(page, size)).map(CategoryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getTeamCategories(User viewer, Team team, int page, int size) {
        Pageable pageable = toPageable(page, size);
        return categoryRepository.findViewableTeam(team.getId(), viewer.getId(), pageable).map(CategoryResponse::from);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Category category) {
        return CategoryResponse.from(category);
    }

    // 공개 범위 변경은 저장 즉시 적용 (소속 투두는 카테고리 공개 범위를 따르므로 함께 적용됨)
    public CategoryResponse updateCategory(Category category, String name, String color, Visibility visibility) {
        category.updateCategory(name, color, visibility);
        return CategoryResponse.from(category);
    }

    // 소속 투두는 같은 소유자의 다른 카테고리로 이동, 같은 소유자의 다른 카테고리가 없으면 삭제 불가
    public Category getMoveTarget(Category category, Long moveToCategoryId) {
        if (category.getId().equals(moveToCategoryId)) {
            throw new CommonException(CategoryErrorCode.INVALID_MOVE_TARGET);
        }
        return categoryRepository.findWithOwnerById(moveToCategoryId)
                .filter(category::isSameOwner)
                .orElseThrow(() -> new CommonException(CategoryErrorCode.INVALID_MOVE_TARGET));
    }

    // 소속 투두 이동은 호출 측(Facade)에서 먼저 수행, 열람 권한은 DB가 연쇄 삭제
    public void deleteCategory(Category category) {
        categoryRepository.deleteById(category.getId());
    }

    // 팀 삭제 시 팀원 개별 열람 권한 삭제 (역할 열람 권한은 복구를 위해 유지)
    public void deleteAllMemberPermissionsByTeam(Team team) {
        categoryPermissionRepository.deleteAllMemberPermissionsByTeam(team);
    }

    @Transactional(readOnly = true)
    public Page<CategoryPermissionResponse> getPermissions(Category category, int page, int size) {
        Pageable pageable = toPageable(page, size);
        return categoryPermissionRepository.findPageByCategory(category, pageable);
    }

    // PRIVATE 팀 카테고리의 열람 허용 대상을 요청 목록으로 교체
    // roles·teamMembers는 호출 측이 조회한 요청 대상 (역할은 roleIds, 팀원은 members 기준)
    public void updatePermissions(Category category, Set<Long> roleIds, List<TeamRole> roles, List<User> members, List<TeamMember> teamMembers) {
        if (roles.size() != roleIds.size()) {
            throw new CommonException(TeamErrorCode.ROLE_NOT_FOUND);
        }
        // 지정한 사용자가 모두 팀원인지 확인
        if (teamMembers.size() != members.stream().distinct().count()) {
            throw new CommonException(TeamErrorCode.MEMBER_NOT_FOUND);
        }

        categoryPermissionRepository.deleteAllByCategory(category);
        // 대상 수와 무관하게 INSERT 1회
        List<Object[]> rows = new ArrayList<>();
        roles.forEach(role -> rows.add(new Object[]{category.getId(), role.getId(), null}));
        teamMembers.forEach(member -> rows.add(new Object[]{category.getId(), null, member.getId()}));
        jdbcBulkInserter.insert("category_permissions", List.of("category_id", "role_id", "member_id"), rows);
    }

    // 소유자가 탈퇴했거나 정지(BANNED)됐거나 팀이 삭제된 카테고리는 존재하지 않는 것으로 처리
    @Transactional(readOnly = true)
    public Category getActiveCategory(Long categoryId) {
        Category category = categoryRepository.findWithOwnerById(categoryId).orElseThrow(
                () -> new CommonException(CategoryErrorCode.CATEGORY_NOT_FOUND)
        );
        boolean ownerDeleted = category.isTeamCategory()
                ? category.getTeam().getDeletedAt() != null
                : category.getUser().getDeletedAt() != null || category.getUser().getStatus() == UserStatus.BANNED;
        if (ownerDeleted) {
            throw new CommonException(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    public void requireTeamCategory(Category category) {
        if (!category.isTeamCategory()) {
            throw new CommonException(CategoryErrorCode.NOT_TEAM_CATEGORY);
        }
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
