package com.backend.orbitflow.domain.suspension.service;

import com.backend.orbitflow.domain.suspension.dto.response.SuspensionListResponse;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionResponse;
import com.backend.orbitflow.domain.suspension.enums.SuspensionStatus;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import com.backend.orbitflow.domain.suspension.dto.response.SuspendedAccountResponse;
import java.util.Optional;

public interface SuspensionService {

    SuspensionResponse suspend(User admin, User target, String reason, LocalDateTime expiresAt, Long reportId);
    Page<SuspensionListResponse> getSuspensions(SuspensionStatus status, String keyword, int page, int size);
    SuspensionResponse getSuspension(Long suspensionId);
    SuspensionResponse updateSuspension(Long suspensionId, String reason, LocalDateTime expiresAt);
    SuspensionResponse release(User admin, Long suspensionId, String releasedReason);
    void expireAll();
    Optional<SuspendedAccountResponse> findActiveNotice(User user);
}
