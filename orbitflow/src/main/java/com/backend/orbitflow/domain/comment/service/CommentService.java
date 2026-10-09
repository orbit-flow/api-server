package com.backend.orbitflow.domain.comment.service;

import com.backend.orbitflow.domain.comment.dto.response.CommentResponse;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;

public interface CommentService {

    CommentResponse createComment(User actor, Long postId, Long parentCommentId, String content);
    Page<CommentResponse> getComments(User viewer, Long postId, int page, int size);
    CommentResponse updateComment(User actor, Long commentId, String content);
    void deleteComment(User actor, Long commentId);
}
