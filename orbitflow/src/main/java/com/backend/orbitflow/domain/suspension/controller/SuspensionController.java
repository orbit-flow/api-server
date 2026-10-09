package com.backend.orbitflow.domain.suspension.controller;

import com.backend.orbitflow.domain.suspension.dto.request.SuspensionRequest;
import com.backend.orbitflow.domain.suspension.dto.request.SuspensionUpdateRequest;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionListResponse;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionResponse;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/suspensions")
public class SuspensionController {

    // 관리자가 모든 정지 정보를 열람하는 메서드
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<SuspensionListResponse>>> getSuspensions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return null;
    }

    // 특정 정지 정보를 열람하는 메서드
    @GetMapping("/{suspensionId}")
    public ResponseEntity<CommonResponse<SuspensionResponse>> getSuspension(
            @PathVariable Long suspensionId
    ) {
        return null;
    }

    // 정지를 작성하는 메서드
    @PostMapping
    public ResponseEntity<CommonResponse<SuspensionResponse>> createSuspension (
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody SuspensionRequest request
    ) {
        return null;
    }

    //
    @PatchMapping("/{suspensionId}")
    public ResponseEntity<CommonResponse<SuspensionResponse>> updateSuspension (
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody SuspensionUpdateRequest request
    ) {
        return null;
    }
}
