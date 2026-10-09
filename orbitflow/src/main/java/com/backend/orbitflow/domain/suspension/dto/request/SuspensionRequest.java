package com.backend.orbitflow.domain.suspension.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record SuspensionRequest(
        @NotBlank(message = "정지 대상 사용자 uuid는 비어있을 수 없습니다.")
        String userUuid,

        @NotBlank(message = "정지 사유는 비어있을 수 없습니다.")
        @Size(max = 255, message = "정지 사유는 255자 이내여야 합니다.")
        String reason,

        // null이면 영구 정지
        LocalDateTime expiresAt,

        // 위반이 확인된(CONFIRMED) 신고에 따른 정지면 신고 id, 정지 시 SANCTIONED 처리
        Long reportId
) {
}
