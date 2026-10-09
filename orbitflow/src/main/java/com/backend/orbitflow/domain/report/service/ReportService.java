package com.backend.orbitflow.domain.report.service;

import com.backend.orbitflow.domain.report.dto.response.ReportResponse;
import com.backend.orbitflow.domain.report.enums.ReportContentType;
import com.backend.orbitflow.domain.report.enums.ReportStatus;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;

public interface ReportService {

    ReportResponse createReport(User reporter, ReportContentType contentType, Long targetId, User targetUser, String reason);
    Page<ReportResponse> getMyReports(User reporter, int page, int size);

    // 관리자
    Page<ReportResponse> searchReports(ReportStatus status, ReportContentType contentType, int page, int size);
    ReportResponse getReport(Long reportId);
    ReportResponse updateStatus(Long reportId, ReportStatus status);
    void sanction(Long reportId, User suspendedUser);
}
