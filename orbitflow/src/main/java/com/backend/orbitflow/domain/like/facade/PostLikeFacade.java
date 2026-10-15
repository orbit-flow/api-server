package com.backend.orbitflow.domain.like.facade;

import com.backend.orbitflow.domain.like.dto.response.LikeResponse;
import com.backend.orbitflow.domain.like.dto.response.LikerResponse;
import com.backend.orbitflow.domain.like.service.PostLikeService;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.service.CategoryAuthorityService;
import com.backend.orbitflow.domain.category.service.CategoryService;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.post.service.PostService;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 게시글 열람 권한(차단·카테고리 공개 범위)을 먼저 확인한 뒤 좋아요 처리
@Component
@RequiredArgsConstructor
public class PostLikeFacade {

    private final PostLikeService postLikeService;
    private final PostService postService;
    private final BlockService blockService;
    private final CategoryService categoryService;
    private final CategoryAuthorityService categoryAuthorityService;
    private final TeamAuthorityService teamAuthorityService;
    private final FollowService followService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    // 좋아요를 누른 경우에만 게시글 작성자에게 알림 (취소 후 다시 누른 경우는 NotificationService에서 1회만 발송)
    @Transactional
    public LikeResponse toggleLike(AuthUser authUser, Long postId) {
        User me = userService.getActiveByUuid(authUser.getUuid());
        Post post = getViewablePost(me, postId);
        LikeResponse like = postLikeService.toggleLike(me, post);
        if (like.liked()) {
            eventPublisher.publishEvent(NotificationRequest.to(post.getUser(), NotificationType.LIKE, me, post.getId(), null,
                    me.getName() + "님이 게시글을 좋아합니다."));
        }
        return like;
    }

    @Transactional(readOnly = true)
    public PageResponse<LikerResponse> getLikers(AuthUser authUser, Long postId, int page, int size) {
        User me = userService.getActiveByUuid(authUser.getUuid());
        return PageResponse.from(postLikeService.getLikers(me, getViewablePost(me, postId), page, size));
    }

    // 게시글을 조회할 수 있는 사용자만 가능 : 차단 관계 -> 연결된 투두의 카테고리 공개 범위 순으로 조회하고 판정은 PostService
    // PostFacade·CommentFacade·PostLikeFacade·ReportFacade에 같은 헬퍼가 있음 (Facade끼리 참조하지 않음)
    private Post getViewablePost(User viewer, Long postId) {
        Post post = postService.getActivePost(postId);
        postService.checkNotBlocked(post, viewer, blockService.isBlocked(post.getUser(), viewer));
        Category category = categoryService.getActiveCategory(post.getTodo().getCategory().getId());
        postService.checkCategoryViewable(post, viewer, canView(category, viewer));
        return post;
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
