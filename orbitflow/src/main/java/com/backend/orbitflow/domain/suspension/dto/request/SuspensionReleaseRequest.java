package com.backend.orbitflow.domain.suspension.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspensionReleaseRequest(
        @NotBlank(message = "해제 사유는 비어있을 수 없습니다.")
        @Size(max = 255, message = "해제 사유는 255자 이내여야 합니다.")
        String releasedReason
) {
}
