package com.backend.orbitflow.domain.point.controller;

import com.backend.orbitflow.domain.point.dto.response.PointSuccessCode;
import com.backend.orbitflow.domain.point.dto.response.PointTransactionResponse;
import com.backend.orbitflow.domain.point.enums.PointTransactionType;
import com.backend.orbitflow.domain.point.facade.PointFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 플랫폼 관리자 전용 (SecurityConfig에서 /api/admin/** ROLE_ADMIN 제한)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminPointController {

    private final PointFacade pointFacade;

    // 사용자의 포인트 거래 내역 (type 미지정 시 전체)
    @GetMapping("/users/{userUuid}/point-transactions")
    public ResponseEntity<CommonResponse<PageResponse<PointTransactionResponse>>> getUserHistory(
            @PathVariable String userUuid,
            @RequestParam(required = false) PointTransactionType type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PointSuccessCode.GET_POINT_HISTORY,
                        pointFacade.getUserHistory(userUuid, type, page, size)
                ));
    }

    // 무효·부정 출석으로 확정된 출석 적립 거래의 포인트 회수 (거래당 1회, 잔액 부족 시 음수)
    @PostMapping("/point-transactions/{transactionId}/revoke")
    public ResponseEntity<CommonResponse<PointTransactionResponse>> revokeAttendance(
            @PathVariable Long transactionId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PointSuccessCode.POINT_REVOKE,
                        pointFacade.revokeAttendance(transactionId)
                ));
    }
}
