package com.backend.orbitflow.domain.report.dto.request;

import com.backend.orbitflow.domain.report.enums.ReportContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotNull(message = "신고 유형은 비어있을 수 없습니다.")
        ReportContentType contentType,

        // POST·COMMENT 신고 대상 id
        Long targetId,

        // USER 신고 대상 uuid
        String targetUserUuid,

        @NotBlank(message = "신고 사유는 비어있을 수 없습니다.")
        @Size(max = 255, message = "신고 사유는 255자 이내여야 합니다.")
        String reason
) {
}
