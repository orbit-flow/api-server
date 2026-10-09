package com.backend.orbitflow.domain.report.dto.request;

import com.backend.orbitflow.domain.report.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ReportStatusRequest(
        @NotNull(message = "처리 상태는 비어있을 수 없습니다.")
        ReportStatus status
) {
}
