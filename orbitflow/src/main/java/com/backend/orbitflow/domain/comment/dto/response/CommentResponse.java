package com.backend.orbitflow.domain.comment.dto.response;

import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// 댓글 목록에는 대댓글을 포함하지 않고 개수만 제공 (대댓글은 GET /api/comments/{commentId}/replies 로 페이지 조회)
// replyCount : 최상위 댓글의 대댓글 수 (대댓글이면 0)
public record CommentResponse(
        Long id,
        Long postId,
        Long parentCommentId,
        String content,
        Author author,
        long replyCount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {

    public record Author(String uuid, String name, String profileImage) {
    }

    public static CommentResponse of(Comment comment, long replyCount) {
        User user = comment.getUser();
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.isReply() ? comment.getParentComment().getId() : null,
                comment.getContent(),
                new Author(user.getUuid(), user.getName(), user.getProfileImage()),
                comment.isReply() ? 0 : replyCount,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    public static CommentResponse from(Comment comment) {
        return of(comment, 0);
    }
}
