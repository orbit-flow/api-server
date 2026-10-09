package com.backend.orbitflow.domain.suspension.dto.response;

import com.backend.orbitflow.domain.suspension.entity.Suspension;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// 정지 안내 화면용 : 정지 계정 로그인 응답, 소셜 로그인 정지 안내 조회
public record SuspendedAccountResponse(
        String reason,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime suspendedAt,
        // null이면 영구 정지
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expiresAt
) {

    public static SuspendedAccountResponse from(Suspension suspension) {
        return new SuspendedAccountResponse(suspension.getReason(), suspension.getCreatedAt(), suspension.getExpiresAt());
    }
}
