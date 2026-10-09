package com.backend.orbitflow.domain.like.dto.response;

import com.backend.orbitflow.domain.like.entity.PostLike;
import com.backend.orbitflow.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// 좋아요 누른 사용자
public record LikerResponse(
        String uuid,
        String name,
        String profileImage,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime likedAt
) {

    public static LikerResponse from(PostLike like) {
        User user = like.getUser();
        return new LikerResponse(
                user.getUuid(),
                user.getName(),
                user.getProfileImage(),
                like.getCreatedAt()
        );
    }
}
