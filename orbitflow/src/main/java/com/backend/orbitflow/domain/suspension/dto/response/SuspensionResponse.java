package com.backend.orbitflow.domain.suspension.dto.response;

import com.backend.orbitflow.domain.suspension.entity.Suspension;
import com.backend.orbitflow.domain.suspension.enums.SuspensionStatus;
import com.backend.orbitflow.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record SuspensionResponse(
        Long id,
        UserInfo user,
        SuspensionStatus status,
        String reason,
        // null이면 영구 정지
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expiresAt,
        UserInfo suspendedBy,
        UserInfo releasedBy,
        String releasedReason,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime releasedAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public record UserInfo(String uuid, String name, String email) {

        static UserInfo from(User user) {
            return user == null ? null : new UserInfo(user.getUuid(), user.getName(), user.getEmail());
        }
    }

    public static SuspensionResponse from(Suspension suspension) {
        return new SuspensionResponse(
                suspension.getId(),
                UserInfo.from(suspension.getUser()),
                suspension.getState(),
                suspension.getReason(),
                suspension.getExpiresAt(),
                UserInfo.from(suspension.getSuspendedBy()),
                UserInfo.from(suspension.getReleasedBy()),
                suspension.getReleasedReason(),
                suspension.getReleasedAt(),
                suspension.getCreatedAt()
        );
    }
}
