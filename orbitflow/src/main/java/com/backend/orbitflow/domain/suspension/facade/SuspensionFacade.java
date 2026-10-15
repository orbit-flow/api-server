package com.backend.orbitflow.domain.suspension.facade;

import com.backend.orbitflow.domain.auth.service.TokenService;
import com.backend.orbitflow.domain.report.service.ReportService;
import com.backend.orbitflow.domain.suspension.dto.request.SuspensionReleaseRequest;
import com.backend.orbitflow.domain.suspension.dto.request.SuspensionRequest;
import com.backend.orbitflow.domain.suspension.dto.request.SuspensionUpdateRequest;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionListResponse;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionResponse;
import com.backend.orbitflow.domain.suspension.enums.SuspensionStatus;
import com.backend.orbitflow.domain.suspension.service.SuspensionService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SuspensionFacade {

    private final SuspensionService suspensionService;
    private final UserService userService;
    private final ReportService reportService;
    private final TokenService tokenService;

    // 대상 사용자 행 락 이후 조회가 최신 커밋을 보도록 READ COMMITTED, 락·정지 확인·신고 제재·정지 저장은 같은 트랜잭션
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SuspensionResponse suspend(AuthUser authUser, SuspensionRequest request) {
        User admin = userService.getActiveByUuid(authUser.getUuid());
        User target = userService.getByUuidIncludingBanned(request.userUuid());
        userService.lockUser(target.getId());
        suspensionService.checkSuspendable(target, request.expiresAt());
        if (request.reportId() != null) {
            reportService.sanction(request.reportId(), target);
        }
        // 재발급 차단 : DB 반영이 확정된 뒤에만 수행 (access token 차단 표시보다 먼저 등록)
        tokenService.deleteRefreshTokenAfterCommit(target.getUuid());
        return suspensionService.suspend(admin, target, request.reason(), request.expiresAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<SuspensionListResponse> getSuspensions(SuspensionStatus status, String keyword, int page, int size) {
        return PageResponse.from(suspensionService.getSuspensions(status, keyword, page, size));
    }

    @Transactional(readOnly = true)
    public SuspensionResponse getSuspension(Long suspensionId) {
        return suspensionService.getSuspension(suspensionId);
    }

    @Transactional
    public SuspensionResponse updateSuspension(Long suspensionId, SuspensionUpdateRequest request) {
        return suspensionService.updateSuspension(suspensionId, request.reason(), request.expiresAt());
    }

    @Transactional
    public SuspensionResponse release(AuthUser authUser, Long suspensionId, SuspensionReleaseRequest request) {
        return suspensionService.release(userService.getActiveByUuid(authUser.getUuid()), suspensionId, request.releasedReason());
    }
}
