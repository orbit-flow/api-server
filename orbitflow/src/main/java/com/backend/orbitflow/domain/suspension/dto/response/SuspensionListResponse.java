package com.backend.orbitflow.domain.suspension.dto.response;

import com.backend.orbitflow.domain.suspension.enums.SuspensionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// 관리자 정지 목록 한 줄 (SuspensionRepository에서 바로 생성)
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

}
