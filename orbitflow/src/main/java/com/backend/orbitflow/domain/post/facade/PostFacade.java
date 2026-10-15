package com.backend.orbitflow.domain.post.facade;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.service.CategoryAuthorityService;
import com.backend.orbitflow.domain.category.service.CategoryService;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.comment.service.CommentService;
import com.backend.orbitflow.domain.like.service.PostLikeService;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.post.dto.request.PostCreateRequest;
import com.backend.orbitflow.domain.post.dto.request.PostUpdateRequest;
import com.backend.orbitflow.domain.post.dto.response.PostResponse;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.service.PostService;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.service.TodoService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PostFacade {

    private final PostService postService;
    private final UserService userService;
    private final TodoService todoService;
    private final CategoryAuthorityService categoryAuthorityService;
    private final FollowService followService;
    private final CategoryService categoryService;
    private final TeamAuthorityService teamAuthorityService;
    private final BlockService blockService;
    private final CommentService commentService;
    private final PostLikeService postLikeService;
    private final ApplicationEventPublisher eventPublisher;

    // 팔로우 계정의 게시글 작성 알림 : 알림을 켠 팔로워 중 게시글을 조회할 수 있는 사용자에게
    @Transactional
    public PostResponse createPost(AuthUser authUser, Long todoId, PostCreateRequest request, List<MultipartFile> images) {
        User me = me(authUser);
        Todo todo = postService.requireTodo(todoService.findTodo(todoId), false);
        Category category = categoryService.getActiveCategory(todo.getCategory().getId());
        boolean teamMember = teamAuthorityService.findMember(category.getTeam(), me).isPresent();
        PostResponse post = postService.createPost(me, todo, category, teamMember, request.content(), images);
        findNotifiableViewers(todo.getCategory(), me).forEach(receivers ->
                eventPublisher.publishEvent(new NotificationRequest(receivers, NotificationType.NEWPOST, me, post.id(), null,
                        me.getName() + "님이 새 게시글을 작성했습니다.")));
        return post;
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(AuthUser authUser, Long postId) {
        User me = me(authUser);
        Post post = getViewablePost(me, postId);
        return postService.getPost(post,
                postLikeService.countByPost(post, me), commentService.countByPost(post, me), postLikeService.isLiked(post, me));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getTodoPosts(AuthUser authUser, Long todoId, int page, int size) {
        User me = me(authUser);
        Todo todo = postService.requireTodo(todoService.findTodo(todoId), true);
        Category category = categoryService.getActiveCategory(todo.getCategory().getId());
        boolean canViewCategory = canView(category, me);
        return PageResponse.from(toResponses(postService.findTodoPosts(me, todo, canViewCategory, page, size), me));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getUserPosts(AuthUser authUser, String userUuid, int page, int size) {
        User me = me(authUser);
        User author = userService.getByUuid(userUuid);
        boolean blocked = blockService.isBlocked(author, me);
        return PageResponse.from(toResponses(postService.findUserPosts(me, author, blocked, page, size), me));
    }

    @Transactional
    public PostResponse updatePost(AuthUser authUser, Long postId, PostUpdateRequest request, List<MultipartFile> images) {
        User me = me(authUser);
        Post post = postService.getActivePost(postId);
        postService.checkAuthor(post, me);
        return postService.updatePost(me, post, request.content(), request.keepImageUrls(), images,
                postLikeService.countByPost(post, me), commentService.countByPost(post, me), postLikeService.isLiked(post, me));
    }

    @Transactional
    public void deletePost(AuthUser authUser, Long postId) {
        postService.deletePost(me(authUser), postId);
    }

    // 게시글을 조회할 수 있는 사용자만 댓글·대댓글·좋아요 가능 : 차단 관계 -> 연결된 투두의 카테고리 공개 범위 순으로 조회하고 판정은 PostService
    // CommentFacade·PostLikeFacade·ReportFacade에도 같은 헬퍼가 있음 (Facade끼리 참조하지 않음)
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

    // 알림을 켠 followee의 팔로워 중 카테고리를 볼 수 있는 사용자 (id → uuid, 팔로워 FOLLOWER_CHUNK명 단위 묶음)
    private List<Map<Long, String>> findNotifiableViewers(Category category, User followee) {
        List<Map<Long, String>> chunks = new ArrayList<>();
        if (!categoryAuthorityService.hasNotifiableFollowers(category)) {
            return chunks;
        }
        Set<Long> viewerIds = categoryAuthorityService.findNotifiableViewerIds(category);
        long afterId = 0L;
        while (true) {
            Map<Long, String> followers = followService.findNotifiableFollowers(followee, afterId, CategoryAuthorityService.FOLLOWER_CHUNK);
            for (Long followerId : followers.keySet()) {
                afterId = followerId;
            }
            Map<Long, String> receivers = categoryAuthorityService.filterNotifiableViewers(followers, viewerIds);
            if (!receivers.isEmpty()) {
                chunks.add(receivers);
            }
            if (followers.size() < CategoryAuthorityService.FOLLOWER_CHUNK) {
                return chunks;
            }
        }
    }

    // 좋아요·댓글 수(탈퇴한 사용자, 요청자와 차단 관계인 사용자 제외)와 요청자의 좋아요 여부를 게시글 묶음 단위로 조회 (N+1 방지)
    private Page<PostResponse> toResponses(Page<Post> posts, User viewer) {
        List<Post> content = posts.getContent();
        if (content.isEmpty()) {
            return postService.toResponses(posts, Map.of(), Map.of(), Set.of());
        }
        return postService.toResponses(posts,
                postLikeService.countByPostIn(content, viewer),
                commentService.countByPostIn(content, viewer),
                postLikeService.findLikedPostIds(content, viewer));
    }

    private User me(AuthUser authUser) {
        return userService.getActiveByUuid(authUser.getUuid());
    }
}
