package com.backend.orbitflow.domain.comment.facade;

import com.backend.orbitflow.domain.comment.dto.request.CommentCreateRequest;
import com.backend.orbitflow.domain.comment.dto.request.CommentUpdateRequest;
import com.backend.orbitflow.domain.comment.dto.response.CommentResponse;
import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.comment.service.CommentService;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.service.PostService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 댓글 작성·조회는 게시글 열람 권한(차단·카테고리 공개 범위)을 먼저 확인
@Component
@RequiredArgsConstructor
public class CommentFacade {

    private final CommentService commentService;
    private final PostService postService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    // 게시글 댓글 활동은 게시글 작성자에게 알림
    @Transactional
    public CommentResponse createComment(AuthUser authUser, Long postId, CommentCreateRequest request) {
        User me = me(authUser);
        Post post = postService.getViewablePost(me, postId);
        CommentResponse comment = commentService.createComment(me, post, request.parentCommentId(), request.content());
        String content = request.parentCommentId() != null
                ? me.getName() + "님이 게시글에 답글을 남겼습니다."
                : me.getName() + "님이 게시글에 댓글을 남겼습니다.";
        eventPublisher.publishEvent(NotificationRequest.to(post.getUser(), NotificationType.NEWCOMMENT, me, post.getId(), null, content));
        return comment;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(AuthUser authUser, Long postId, int page, int size) {
        User me = me(authUser);
        return PageResponse.from(commentService.getComments(me, postService.getViewablePost(me, postId), page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getReplies(AuthUser authUser, Long commentId, int page, int size) {
        User me = me(authUser);
        Comment parent = commentService.getTopLevelComment(commentId);
        postService.getViewablePost(me, parent.getPost().getId());
        return PageResponse.from(commentService.getReplies(me, parent, page, size));
    }

    @Transactional
    public CommentResponse updateComment(AuthUser authUser, Long commentId, CommentUpdateRequest request) {
        return commentService.updateComment(me(authUser), commentId, request.content());
    }

    @Transactional
    public void deleteComment(AuthUser authUser, Long commentId) {
        commentService.deleteComment(me(authUser), commentId);
    }

    private User me(AuthUser authUser) {
        return userService.getByUuid(authUser.getUuid());
    }
}
