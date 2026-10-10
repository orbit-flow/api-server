package com.backend.orbitflow.domain.report.facade;

import com.backend.orbitflow.domain.report.dto.request.ReportRequest;
import com.backend.orbitflow.domain.report.dto.request.ReportStatusRequest;
import com.backend.orbitflow.domain.report.dto.response.ReportResponse;
import com.backend.orbitflow.domain.report.enums.ReportContentType;
import com.backend.orbitflow.domain.report.enums.ReportStatus;
import com.backend.orbitflow.domain.report.service.ReportService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.backend.orbitflow.domain.report.dto.response.ReportListResponse;

@Component
@RequiredArgsConstructor
public class ReportFacade {

    private final ReportService reportService;
    private final UserService userService;

    // 신고자 행 락(PESSIMISTIC_WRITE)이 최신 상태를 보도록 READ_COMMITTED 필수
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReportResponse createReport(AuthUser authUser, ReportRequest request) {
        User targetUser = request.contentType() == ReportContentType.USER && request.targetUserUuid() != null
                ? userService.getByUuid(request.targetUserUuid())
                : null;
        return reportService.createReport(
                userService.getByUuid(authUser.getUuid()),
                request.contentType(),
                request.targetId(),
                targetUser,
                request.reason()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportListResponse> getMyReports(AuthUser authUser, int page, int size) {
        return PageResponse.from(reportService.getMyReports(userService.getByUuid(authUser.getUuid()), page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportListResponse> searchReports(ReportStatus status, ReportContentType contentType, int page, int size) {
        return PageResponse.from(reportService.searchReports(status, contentType, page, size));
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(Long reportId) {
        return reportService.getReport(reportId);
    }

    @Transactional
    public ReportResponse updateStatus(Long reportId, ReportStatusRequest request) {
        return reportService.updateStatus(reportId, request.status());
    }
}
