package com.backend.orbitflow.domain.suspension.dto.response;

import com.backend.orbitflow.domain.suspension.entity.Suspension;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// 정지 계정 로그인 시 에러 응답 data (사용자에게 정지 사유·기간 안내)
public record SuspendedAccountResponse(
        String reason,
        // null이면 영구 정지
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expiresAt
) {

    public static SuspendedAccountResponse from(Suspension suspension) {
        return new SuspendedAccountResponse(suspension.getReason(), suspension.getExpiresAt());
    }
}
