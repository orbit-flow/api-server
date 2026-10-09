package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.enums.Visibility;
import com.backend.orbitflow.domain.category.error.CategoryErrorCode;
import com.backend.orbitflow.domain.category.repository.CategoryPermissionRepository;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

// 허용되지 않은 조회는 진입 경로와 무관하게 거부
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryAuthorityServiceImpl implements CategoryAuthorityService {

    private final CategoryPermissionRepository categoryPermissionRepository;
    private final TeamAuthorityService teamAuthorityService;
    private final FollowService followService;
    private final BlockService blockService;

    public boolean canView(Category category, User viewer) {
        if (category.isTeamCategory()) {
            return filterViewableTeam(category.getTeam(), viewer, List.of(category)).size() == 1;
        }
        return filterViewablePersonal(category.getUser(), viewer, List.of(category)).size() == 1;
    }

    public void checkView(Category category, User viewer) {
        if (!canView(category, viewer)) {
            throw new CommonException(CategoryErrorCode.CATEGORY_ACCESS_DENIED);
        }
    }

    // 개인 카테고리는 소유자만, 팀 카테고리는 MANAGE_CATEGORIES 권한 보유자만
    public void checkEdit(Category category, User actor) {
        if (category.isTeamCategory()) {
            teamAuthorityService.checkPermission(category.getTeam(), actor, TeamPermission.MANAGE_CATEGORIES);
            return;
        }
        if (!category.isOwnedBy(actor)) {
            throw new CommonException(CategoryErrorCode.CATEGORY_EDIT_DENIED);
        }
    }

    // 차단 관계가 있으면 모두 비공개, 비밀계정이면 PUBLIC도 FOLLOWER로 제한 (가장 제한적인 범위 적용)
    public List<Category> filterViewablePersonal(User owner, User viewer, List<Category> categories) {
        if (owner.getId().equals(viewer.getId())) {
            return categories;
        }
        if (blockService.isBlocked(owner, viewer)) {
            return List.of();
        }
        boolean isFollower = followService.getFollowState(viewer, owner) == FollowState.ACCEPTED;
        return categories.stream()
                .filter(category -> {
                    Visibility visibility = owner.isPrivate()
                            ? category.getVisibility().mostRestrictive(Visibility.FOLLOWER)
                            : category.getVisibility();
                    return switch (visibility) {
                        case PUBLIC -> true;
                        case FOLLOWER -> isFollower;
                        case PRIVATE -> false;
                    };
                })
                .toList();
    }

    // PUBLIC 누구나, FOLLOWER 모든 팀원, PRIVATE MANAGE_CATEGORIES 보유자 또는 지정된 역할·팀원
    public List<Category> filterViewableTeam(Team team, User viewer, List<Category> categories) {
        Optional<TeamMember> member = teamAuthorityService.findMember(team, viewer);
        if (member.isEmpty()) {
            return categories.stream()
                    .filter(category -> category.getVisibility() == Visibility.PUBLIC)
                    .toList();
        }
        boolean canManage = TeamPermission.MANAGE_CATEGORIES.isGranted(
                teamAuthorityService.getPermissionMask(team, member.get())
        );
        Set<Long> allowedIds = canManage ? Set.of() : categoryPermissionRepository.findAllowedCategoryIds(team, member.get());
        return categories.stream()
                .filter(category -> category.getVisibility() != Visibility.PRIVATE
                        || canManage
                        || allowedIds.contains(category.getId()))
                .toList();
    }
}
