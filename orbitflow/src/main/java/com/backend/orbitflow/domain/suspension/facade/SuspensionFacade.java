package com.backend.orbitflow.domain.suspension.facade;

import com.backend.orbitflow.domain.suspension.dto.request.SuspensionReleaseRequest;
import com.backend.orbitflow.domain.suspension.dto.request.SuspensionRequest;
import com.backend.orbitflow.domain.suspension.dto.request.SuspensionUpdateRequest;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionListResponse;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionResponse;
import com.backend.orbitflow.domain.suspension.enums.SuspensionStatus;
import com.backend.orbitflow.domain.suspension.service.SuspensionService;
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

    // 대상 사용자 행 락 이후 조회가 최신 커밋을 보도록 READ COMMITTED
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SuspensionResponse suspend(AuthUser authUser, SuspensionRequest request) {
        return suspensionService.suspend(
                userService.getByUuid(authUser.getUuid()),
                userService.getByUuidIncludingBanned(request.userUuid()),
                request.reason(),
                request.expiresAt(),
                request.reportId()
        );
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
        return suspensionService.release(userService.getByUuid(authUser.getUuid()), suspensionId, request.releasedReason());
    }
}
