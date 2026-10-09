package com.backend.orbitflow.domain.report.controller;

import com.backend.orbitflow.domain.report.dto.request.ReportStatusRequest;
import com.backend.orbitflow.domain.report.dto.response.ReportResponse;
import com.backend.orbitflow.domain.report.dto.response.ReportSuccessCode;
import com.backend.orbitflow.domain.report.enums.ReportContentType;
import com.backend.orbitflow.domain.report.enums.ReportStatus;
import com.backend.orbitflow.domain.report.facade.ReportFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 플랫폼 관리자 전용 (SecurityConfig에서 /api/admin/** ROLE_ADMIN 제한)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportFacade reportFacade;

    // status·contentType 미지정 시 전체
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<ReportResponse>>> searchReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportContentType contentType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ReportSuccessCode.GET_REPORT_LIST,
                        reportFacade.searchReports(status, contentType, page, size)
                ));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<CommonResponse<ReportResponse>> getReport(
            @PathVariable Long reportId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ReportSuccessCode.GET_REPORT_INFO,
                        reportFacade.getReport(reportId)
                ));
    }

    // RECEIVED -> CONFIRMED -> SANCTIONED, 처리 완료 전 REJECTED 가능
    @PatchMapping("/{reportId}/status")
    public ResponseEntity<CommonResponse<ReportResponse>> updateStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportStatusRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ReportSuccessCode.REPORT_STATUS_UPDATE,
                        reportFacade.updateStatus(reportId, request)
                ));
    }
}
