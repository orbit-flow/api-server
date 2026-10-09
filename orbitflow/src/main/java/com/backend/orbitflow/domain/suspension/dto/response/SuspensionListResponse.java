package com.backend.orbitflow.domain.suspension.dto.response;

import com.backend.orbitflow.domain.suspension.entity.Suspension;
import com.backend.orbitflow.domain.suspension.enums.SuspensionStatus;
import com.backend.orbitflow.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record SuspensionListResponse(
        Long id,
        String userUuid,
        String userName,
        String userEmail,
        SuspensionStatus status,
        String reason,
        // null이면 영구 정지
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expiresAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public static SuspensionListResponse from(Suspension suspension) {
        User user = suspension.getUser();
        return new SuspensionListResponse(
                suspension.getId(),
                user.getUuid(),
                user.getName(),
                user.getEmail(),
                suspension.getState(),
                suspension.getReason(),
                suspension.getExpiresAt(),
                suspension.getCreatedAt()
        );
    }
}
