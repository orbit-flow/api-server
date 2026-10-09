package com.backend.orbitflow.domain.comment.dto.response;

import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

// replies : 최상위 댓글에만 포함 (대댓글이면 null)
public record CommentResponse(
        Long id,
        Long postId,
        Long parentCommentId,
        String content,
        Author author,
        List<CommentResponse> replies,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {

    public record Author(String uuid, String name, String profileImage) {
    }

    public static CommentResponse of(Comment comment, List<CommentResponse> replies) {
        User user = comment.getUser();
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.isReply() ? comment.getParentComment().getId() : null,
                comment.getContent(),
                new Author(user.getUuid(), user.getName(), user.getProfileImage()),
                comment.isReply() ? null : replies,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    public static CommentResponse from(Comment comment) {
        return of(comment, List.of());
    }
}
