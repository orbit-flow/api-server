package com.backend.orbitflow.domain.post.dto.response;

import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.entity.PostImage;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String content,
        List<String> imageUrls,
        long likeCount,
        long commentCount,
        // 요청자가 좋아요를 눌렀는지
        boolean liked,
        Author author,
        TodoInfo todo,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {

    public record Author(String uuid, String name, String profileImage) {
    }

    // deleted : 연결된 투두가 삭제된 경우 true (게시글은 유지)
    public record TodoInfo(Long id, Long categoryId, String name, boolean isCompleted, boolean deleted) {
    }

    public static PostResponse of(Post post, List<PostImage> images, long likeCount, long commentCount, boolean liked) {
        User user = post.getUser();
        Todo todo = post.getTodo();
        return new PostResponse(
                post.getId(),
                post.getContent(),
                images.stream()
                        .map(PostImage::getImageUrl)
                        .toList(),
                likeCount,
                commentCount,
                liked,
                new Author(user.getUuid(), user.getName(), user.getProfileImage()),
                new TodoInfo(todo.getId(), todo.getCategory().getId(), todo.getName(), todo.isCompleted(), todo.isDeleted()),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
