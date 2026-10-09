package com.backend.orbitflow.domain.user.dto.response;

import java.time.LocalDateTime;

import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

public record UserResponse(

    String name,
    String email,
    String profileImage,
    String introduce,
    UserStatus status,
    boolean isPrivate,
    boolean allowNonFollowChatInvite,
    // false면 소셜 로그인 전용 계정 (비밀번호 설정 가능)
    boolean hasPassword,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
            user.getName(),
            user.getEmail(),
            user.getProfileImage(),
            user.getIntroduce(),
            user.getStatus(),
            user.isPrivate(),
            user.isAllowNonFollowChatInvite(),
            user.getPassword() != null,
            user.getCreatedAt()
        );
    }
}
