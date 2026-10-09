package com.backend.orbitflow.domain.suspension.controller;

import com.backend.orbitflow.domain.suspension.dto.request.SuspensionReleaseRequest;
import com.backend.orbitflow.domain.suspension.dto.request.SuspensionRequest;
import com.backend.orbitflow.domain.suspension.dto.request.SuspensionUpdateRequest;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionListResponse;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionResponse;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionSuccessCode;
import com.backend.orbitflow.domain.suspension.enums.SuspensionStatus;
import com.backend.orbitflow.domain.suspension.facade.SuspensionFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 플랫폼 관리자 전용 (SecurityConfig에서 /api/admin/** ROLE_ADMIN 제한)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/suspensions")
public class SuspensionController {

    private final SuspensionFacade suspensionFacade;

    // 관리자가 모든 정지 정보를 열람하는 메서드 (status 필터, keyword : 사용자 이름·이메일)
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<SuspensionListResponse>>> getSuspensions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SuspensionStatus status
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        SuspensionSuccessCode.GET_SUSPENSION_LIST,
                        suspensionFacade.getSuspensions(status, keyword, page, size)
                ));
    }

    // 특정 정지 정보를 열람하는 메서드
    @GetMapping("/{suspensionId}")
    public ResponseEntity<CommonResponse<SuspensionResponse>> getSuspension(
            @PathVariable Long suspensionId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        SuspensionSuccessCode.GET_SUSPENSION_INFO,
                        suspensionFacade.getSuspension(suspensionId)
                ));
    }

    // 정지를 작성하는 메서드
    @PostMapping
    public ResponseEntity<CommonResponse<SuspensionResponse>> createSuspension(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody SuspensionRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        SuspensionSuccessCode.SUSPENSION_CREATE,
                        suspensionFacade.suspend(authUser, request)
                ));
    }

    // 진행 중인 정지의 사유·기간을 수정하는 메서드
    @PatchMapping("/{suspensionId}")
    public ResponseEntity<CommonResponse<SuspensionResponse>> updateSuspension(
            @PathVariable Long suspensionId,
            @Valid @RequestBody SuspensionUpdateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        SuspensionSuccessCode.SUSPENSION_UPDATE,
                        suspensionFacade.updateSuspension(suspensionId, request)
                ));
    }

    // 정지를 해제하는 메서드
    @PatchMapping("/{suspensionId}/release")
    public ResponseEntity<CommonResponse<SuspensionResponse>> releaseSuspension(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long suspensionId,
            @Valid @RequestBody SuspensionReleaseRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        SuspensionSuccessCode.SUSPENSION_RELEASE,
                        suspensionFacade.release(authUser, suspensionId, request)
                ));
    }
}
