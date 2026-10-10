package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.enums.Visibility;
import com.backend.orbitflow.domain.category.error.CategoryErrorCode;
import com.backend.orbitflow.domain.category.repository.CategoryPermissionRepository;
import com.backend.orbitflow.domain.category.repository.CategoryRepository;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// 허용되지 않은 조회는 진입 경로와 무관하게 거부
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryAuthorityServiceImpl implements CategoryAuthorityService {

    private static final int FOLLOWER_CHUNK = 500;

    private final CategoryPermissionRepository categoryPermissionRepository;
    private final CategoryRepository categoryRepository;
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

    // 팀 카테고리를 볼 수 있는 팀 구성원의 사용자 id (구성원마다 권한을 조회하지 않음)
    public Set<Long> findViewerUserIds(Category teamCategory) {
        return categoryRepository.findViewerUserIds(teamCategory.getId());
    }

    // 알림을 켠 팔로워 중 카테고리를 볼 수 있는 사용자 (id → uuid, 팔로워 500명 단위로 나눈 묶음, 조회 권한 쿼리는 최대 1회)
    // 팔로워는 수락된 팔로우만 포함하고 차단 시 팔로우가 삭제되므로, 개인 카테고리는 공개 범위만으로 판정
    // (팔로워는 FOLLOWER 범위를 볼 수 있고, 비밀계정의 PUBLIC도 FOLLOWER로 제한될 뿐이므로 PRIVATE만 제외)
    public List<Map<Long, String>> findNotifiableViewers(Category category, User followee) {
        List<Map<Long, String>> chunks = new ArrayList<>();
        if (!category.isTeamCategory() && category.getVisibility() == Visibility.PRIVATE) {
            return chunks;
        }
        // null이면 팔로워 전원이 조회 가능
        Set<Long> viewerIds = category.isTeamCategory() && category.getVisibility() != Visibility.PUBLIC
                ? findViewerUserIds(category)
                : null;
        long afterId = 0L;
        while (true) {
            Map<Long, String> followers = followService.findNotifiableFollowers(followee, afterId, FOLLOWER_CHUNK);
            Map<Long, String> receivers = new LinkedHashMap<>();
            for (Map.Entry<Long, String> follower : followers.entrySet()) {
                afterId = follower.getKey();
                if (viewerIds == null || viewerIds.contains(follower.getKey())) {
                    receivers.put(follower.getKey(), follower.getValue());
                }
            }
            if (!receivers.isEmpty()) {
                chunks.add(receivers);
            }
            if (followers.size() < FOLLOWER_CHUNK) {
                return chunks;
            }
        }
    }
}
