package com.backend.orbitflow.domain.comment.service;

import com.backend.orbitflow.domain.comment.dto.response.CommentResponse;
import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

// 게시글 열람 권한은 호출 측(CommentFacade)에서 PostService로 확인한 뒤 전달
public interface CommentService {

    CommentResponse createComment(User actor, Post post, Long parentCommentId, String content);
    Page<CommentResponse> getComments(User viewer, Post post, int page, int size);
    Comment getTopLevelComment(Long commentId);
    Page<CommentResponse> getReplies(User viewer, Comment parent, int page, int size);
    CommentResponse updateComment(User actor, Long commentId, String content);
    void deleteComment(User actor, Long commentId);
    Optional<Comment> findById(Long commentId);

    // 게시글 응답 조립용 (PostService에서 사용)
    long countByPost(Post post, User viewer);
    Map<Long, Long> countByPostIn(Collection<Post> posts, User viewer);
}
