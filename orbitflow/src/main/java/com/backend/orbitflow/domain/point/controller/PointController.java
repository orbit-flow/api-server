package com.backend.orbitflow.domain.point.controller;

import com.backend.orbitflow.domain.point.dto.response.AttendanceStatusResponse;
import com.backend.orbitflow.domain.point.dto.response.PointSuccessCode;
import com.backend.orbitflow.domain.point.dto.response.PointTransactionResponse;
import com.backend.orbitflow.domain.point.facade.PointFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointFacade pointFacade;

    // 출석 포인트 적립 (운영일 기준 하루 1회)
    @PostMapping("/attendance")
    public ResponseEntity<CommonResponse<PointTransactionResponse>> attend(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PointSuccessCode.ATTENDANCE_SUCCESS,
                        pointFacade.attend(authUser)
                ));
    }

    @GetMapping("/attendance/today")
    public ResponseEntity<CommonResponse<AttendanceStatusResponse>> getAttendanceStatus(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PointSuccessCode.GET_ATTENDANCE_STATUS,
                        pointFacade.getAttendanceStatus(authUser)
                ));
    }

    // 포인트 거래 내역 (최신순)
    @GetMapping("/transactions")
    public ResponseEntity<CommonResponse<PageResponse<PointTransactionResponse>>> getHistory(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PointSuccessCode.GET_POINT_HISTORY,
                        pointFacade.getHistory(authUser, page, size)
                ));
    }
}
