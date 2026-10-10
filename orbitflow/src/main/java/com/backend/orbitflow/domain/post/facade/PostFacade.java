package com.backend.orbitflow.domain.post.facade;

import com.backend.orbitflow.domain.category.service.CategoryAuthorityService;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.post.dto.request.PostCreateRequest;
import com.backend.orbitflow.domain.post.dto.request.PostUpdateRequest;
import com.backend.orbitflow.domain.post.dto.response.PostResponse;
import com.backend.orbitflow.domain.post.service.PostService;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.service.TodoService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostFacade {

    private final PostService postService;
    private final UserService userService;
    private final TodoService todoService;
    private final CategoryAuthorityService categoryAuthorityService;
    private final ApplicationEventPublisher eventPublisher;

    // 팔로우 계정의 게시글 작성 알림 : 알림을 켠 팔로워 중 게시글을 조회할 수 있는 사용자에게
    @Transactional
    public PostResponse createPost(AuthUser authUser, Long todoId, PostCreateRequest request, List<MultipartFile> images) {
        User me = me(authUser);
        PostResponse post = postService.createPost(me, todoId, request.content(), images);
        Todo todo = todoService.findTodo(todoId).orElseThrow();
        categoryAuthorityService.findNotifiableViewers(todo.getCategory(), me).forEach(receivers ->
                eventPublisher.publishEvent(new NotificationRequest(receivers, NotificationType.NEWPOST, me, post.id(), null,
                        me.getName() + "님이 새 게시글을 작성했습니다.")));
        return post;
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(AuthUser authUser, Long postId) {
        return postService.getPost(me(authUser), postId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getTodoPosts(AuthUser authUser, Long todoId, int page, int size) {
        return PageResponse.from(postService.getTodoPosts(me(authUser), todoId, page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getUserPosts(AuthUser authUser, String userUuid, int page, int size) {
        return PageResponse.from(postService.getUserPosts(me(authUser), userService.getByUuid(userUuid), page, size));
    }

    @Transactional
    public PostResponse updatePost(AuthUser authUser, Long postId, PostUpdateRequest request, List<MultipartFile> images) {
        return postService.updatePost(me(authUser), postId, request.content(), request.keepImageUrls(), images);
    }

    @Transactional
    public void deletePost(AuthUser authUser, Long postId) {
        postService.deletePost(me(authUser), postId);
    }

    private User me(AuthUser authUser) {
        return userService.getByUuid(authUser.getUuid());
    }
}
