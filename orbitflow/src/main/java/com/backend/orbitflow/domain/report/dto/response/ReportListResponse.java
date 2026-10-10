package com.backend.orbitflow.domain.report.dto.response;

import com.backend.orbitflow.domain.report.enums.ReportContentType;
import com.backend.orbitflow.domain.report.enums.ReportStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// 신고 목록 한 줄 (평면 구조, UserReportRepository에서 신고 대상·작성자까지 한 번에 조회해 바로 생성)
// targetId : POST·COMMENT id (USER는 내부 id 비노출로 null)
// targetUserUuid : USER면 대상 사용자, POST·COMMENT면 작성자
// targetSummary : 게시글·댓글 내용 앞부분 또는 사용자 이름
// targetExists : 대상이 삭제되었으면 false
public record ReportListResponse(
        Long id,
        ReportContentType contentType,
        Long targetId,
        String targetUserUuid,
        String targetSummary,
        boolean targetExists,
        String reason,
        ReportStatus status,
        String reporterUuid,
        String reporterName,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {

    private static final int SUMMARY_LENGTH = 50;

    public ReportListResponse {
        if (targetSummary != null && targetSummary.length() > SUMMARY_LENGTH) {
            targetSummary = targetSummary.substring(0, SUMMARY_LENGTH) + "...";
        }
    }
}
