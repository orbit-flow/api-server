package com.backend.orbitflow.domain.report.controller;

import com.backend.orbitflow.domain.report.dto.request.ReportRequest;
import com.backend.orbitflow.domain.report.dto.response.ReportResponse;
import com.backend.orbitflow.domain.report.dto.response.ReportSuccessCode;
import com.backend.orbitflow.domain.report.facade.ReportFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportFacade reportFacade;

    @PostMapping
    public ResponseEntity<CommonResponse<ReportResponse>> createReport(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ReportRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        ReportSuccessCode.REPORT_CREATE,
                        reportFacade.createReport(authUser, request)
                ));
    }

    // 내가 접수한 신고와 처리 상태
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<PageResponse<ReportResponse>>> getMyReports(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ReportSuccessCode.GET_REPORT_LIST,
                        reportFacade.getMyReports(authUser, page, size)
                ));
    }
}
