package com.backend.orbitflow.domain.post.facade;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.facade.CategoryFacade;
import com.backend.orbitflow.domain.category.service.CategoryService;
import com.backend.orbitflow.domain.comment.service.CommentService;
import com.backend.orbitflow.domain.like.service.PostLikeService;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.post.dto.request.PostCreateRequest;
import com.backend.orbitflow.domain.post.dto.request.PostUpdateRequest;
import com.backend.orbitflow.domain.post.dto.response.PostResponse;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.service.PostService;
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

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PostFacade {

    private final PostService postService;
    private final UserService userService;
    private final TodoService todoService;
    private final CategoryFacade categoryFacade;
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
        boolean teamMember = category.isTeamCategory() && teamAuthorityService.findMember(category.getTeam(), me).isPresent();
        PostResponse post = postService.createPost(me, todo, category, teamMember, request.content(), images);
        categoryFacade.findNotifiableViewers(todo.getCategory(), me).forEach(receivers ->
                eventPublisher.publishEvent(new NotificationRequest(receivers, NotificationType.NEWPOST, me, post.id(), null,
                        me.getName() + "님이 새 게시글을 작성했습니다.")));
        return post;
    }

    // 게시글을 조회할 수 있는 사용자만 댓글·대댓글·좋아요 가능 (CommentFacade·PostLikeFacade·ReportFacade에서도 사용)
    // 차단 관계가 우선, 이후 연결된 투두의 카테고리 공개 범위 적용
    @Transactional(readOnly = true)
    public Post getViewablePost(User viewer, Long postId) {
        Post post = postService.getActivePost(postId);
        boolean isAuthor = post.isAuthor(viewer);
        postService.checkNotBlocked(!isAuthor && blockService.isBlocked(post.getUser(), viewer));
        Category category = categoryService.getActiveCategory(post.getTodo().getCategory().getId());
        postService.checkCategoryViewable(isAuthor || categoryFacade.canView(category, viewer));
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
        boolean canViewCategory = categoryFacade.canView(category, me);
        return PageResponse.from(toResponses(postService.findTodoPosts(me, todo, canViewCategory, page, size), me));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getUserPosts(AuthUser authUser, String userUuid, int page, int size) {
        User me = me(authUser);
        User author = userService.getByUuid(userUuid);
        boolean blocked = !me.getId().equals(author.getId()) && blockService.isBlocked(author, me);
        return PageResponse.from(toResponses(postService.findUserPosts(me, author, blocked, page, size), me));
    }

    @Transactional
    public PostResponse updatePost(AuthUser authUser, Long postId, PostUpdateRequest request, List<MultipartFile> images) {
        User me = me(authUser);
        Post post = postService.getActivePost(postId);
        return postService.updatePost(me, post, request.content(), request.keepImageUrls(), images,
                postLikeService.countByPost(post, me), commentService.countByPost(post, me), postLikeService.isLiked(post, me));
    }

    @Transactional
    public void deletePost(AuthUser authUser, Long postId) {
        postService.deletePost(me(authUser), postId);
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
