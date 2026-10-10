package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.enums.Visibility;
import com.backend.orbitflow.domain.category.error.CategoryErrorCode;
import com.backend.orbitflow.domain.category.repository.CategoryPermissionRepository;
import com.backend.orbitflow.domain.category.repository.CategoryRepository;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// 허용되지 않은 조회는 진입 경로와 무관하게 거부
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryAuthorityServiceImpl implements CategoryAuthorityService {

    private final CategoryPermissionRepository categoryPermissionRepository;
    private final CategoryRepository categoryRepository;

    // 차단 관계가 있으면 모두 비공개, 비밀계정이면 PUBLIC도 FOLLOWER로 제한 (가장 제한적인 범위 적용)
    public boolean canViewPersonal(Category category, User viewer, boolean blocked, FollowState followState) {
        User owner = category.getUser();
        if (owner.getId().equals(viewer.getId())) {
            return true;
        }
        if (blocked) {
            return false;
        }
        boolean isFollower = followState == FollowState.ACCEPTED;
        Visibility visibility = owner.isPrivate()
                ? category.getVisibility().mostRestrictive(Visibility.FOLLOWER)
                : category.getVisibility();
        return switch (visibility) {
            case PUBLIC -> true;
            case FOLLOWER -> isFollower;
            case PRIVATE -> false;
        };
    }

    // PUBLIC 누구나, FOLLOWER 모든 팀원, PRIVATE MANAGE_CATEGORIES 보유자 또는 지정된 역할·팀원
    public boolean canViewTeam(Category category, TeamMember member, Integer teamMask) {
        if (member == null) {
            return category.getVisibility() == Visibility.PUBLIC;
        }
        boolean canManage = TeamPermission.MANAGE_CATEGORIES.isGranted(teamMask);
        Set<Long> allowedIds = canManage ? Set.of() : categoryPermissionRepository.findAllowedCategoryIds(category.getTeam(), member);
        return category.getVisibility() != Visibility.PRIVATE
                || canManage
                || allowedIds.contains(category.getId());
    }

    public void checkView(boolean viewable) {
        if (!viewable) {
            throw new CommonException(CategoryErrorCode.CATEGORY_ACCESS_DENIED);
        }
    }

    // 개인 카테고리는 소유자만, 팀 카테고리는 MANAGE_CATEGORIES 권한 보유자만
    public void checkEdit(Category category, User actor, Integer teamMask) {
        if (category.isTeamCategory()) {
            checkManage(teamMask);
            return;
        }
        if (!category.isOwnedBy(actor)) {
            throw new CommonException(CategoryErrorCode.CATEGORY_EDIT_DENIED);
        }
    }

    // 비소속 사용자에게는 팀 존재 여부를 노출하지 않도록 NOT_FOUND 처리 (TeamAuthorityService.checkPermission과 같은 규칙)
    public void checkManage(Integer teamMask) {
        if (teamMask == null) {
            throw new CommonException(TeamErrorCode.TEAM_NOT_FOUND);
        }
        if (!TeamPermission.MANAGE_CATEGORIES.isGranted(teamMask)) {
            throw new CommonException(TeamErrorCode.NO_TEAM_PERMISSION);
        }
    }

    // 팀 카테고리를 볼 수 있는 팀 구성원의 사용자 id (구성원마다 권한을 조회하지 않음)
    public Set<Long> findViewerUserIds(Category teamCategory) {
        return categoryRepository.findViewerUserIds(teamCategory.getId());
    }

    // 알림을 켠 팔로워는 수락된 팔로우만 포함하고 차단 시 팔로우가 삭제되므로, 개인 카테고리는 공개 범위만으로 판정
    // (팔로워는 FOLLOWER 범위를 볼 수 있고, 비밀계정의 PUBLIC도 FOLLOWER로 제한될 뿐이므로 PRIVATE만 제외)
    public boolean hasNotifiableFollowers(Category category) {
        return category.isTeamCategory() || category.getVisibility() != Visibility.PRIVATE;
    }

    // 팀 카테고리의 PUBLIC 외에는 팀 구성원만 조회 가능
    public Set<Long> findNotifiableViewerIds(Category category) {
        return category.isTeamCategory() && category.getVisibility() != Visibility.PUBLIC
                ? findViewerUserIds(category)
                : null;
    }

    public Map<Long, String> filterNotifiableViewers(Map<Long, String> followers, Set<Long> viewerIds) {
        Map<Long, String> receivers = new LinkedHashMap<>();
        for (Map.Entry<Long, String> follower : followers.entrySet()) {
            if (viewerIds == null || viewerIds.contains(follower.getKey())) {
                receivers.put(follower.getKey(), follower.getValue());
            }
        }
        return receivers;
    }
}
