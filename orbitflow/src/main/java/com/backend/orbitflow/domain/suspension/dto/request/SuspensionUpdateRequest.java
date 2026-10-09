package com.backend.orbitflow.domain.suspension.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// 진행 중인 정지의 사유·기간 변경
public record SuspensionUpdateRequest(
        @NotBlank(message = "정지 사유는 비어있을 수 없습니다.")
        @Size(max = 255, message = "정지 사유는 255자 이내여야 합니다.")
        String reason,

        // null이면 영구 정지
        LocalDateTime expiresAt
) {
}
