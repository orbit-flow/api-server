package com.backend.orbitflow.domain.comment.facade;

import com.backend.orbitflow.domain.comment.dto.request.CommentCreateRequest;
import com.backend.orbitflow.domain.comment.dto.request.CommentUpdateRequest;
import com.backend.orbitflow.domain.comment.dto.response.CommentResponse;
import com.backend.orbitflow.domain.comment.service.CommentService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CommentFacade {

    private final CommentService commentService;
    private final UserService userService;

    @Transactional
    public CommentResponse createComment(AuthUser authUser, Long postId, CommentCreateRequest request) {
        return commentService.createComment(me(authUser), postId, request.parentCommentId(), request.content());
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(AuthUser authUser, Long postId, int page, int size) {
        return PageResponse.from(commentService.getComments(me(authUser), postId, page, size));
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
