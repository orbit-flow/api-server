package com.backend.orbitflow.domain.report.dto.response;

import com.backend.orbitflow.domain.report.entity.UserReport;
import com.backend.orbitflow.domain.report.enums.ReportContentType;
import com.backend.orbitflow.domain.report.enums.ReportStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        ReportContentType contentType,
        Target target,
        String reason,
        ReportStatus status,
        Reporter reporter,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {

    // id : POST·COMMENT id (USER는 내부 id 비노출로 null)
    // userUuid : USER면 대상 사용자, POST·COMMENT면 작성자
    // exists : 대상이 삭제되었으면 false
    public record Target(Long id, String userUuid, String summary, boolean exists) {

        public static Target missing(Long id) {
            return new Target(id, null, null, false);
        }
    }

    public record Reporter(String uuid, String name) {
    }

    public static ReportResponse of(UserReport report, Target target) {
        return new ReportResponse(
                report.getId(),
                report.getContentType(),
                target,
                report.getReason(),
                report.getStatus(),
                new Reporter(report.getReporter().getUuid(), report.getReporter().getName()),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
