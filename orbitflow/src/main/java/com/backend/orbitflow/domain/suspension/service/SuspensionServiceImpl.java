package com.backend.orbitflow.domain.suspension.service;

import com.backend.orbitflow.domain.auth.service.TokenService;
import com.backend.orbitflow.domain.report.service.ReportService;
import com.backend.orbitflow.domain.suspension.cache.SuspendedUserCache;
import com.backend.orbitflow.domain.suspension.dto.response.SuspendedAccountResponse;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionListResponse;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionResponse;
import com.backend.orbitflow.domain.suspension.entity.Suspension;
import com.backend.orbitflow.domain.suspension.enums.SuspensionStatus;
import com.backend.orbitflow.domain.suspension.error.SuspensionErrorCode;
import com.backend.orbitflow.domain.suspension.repository.SuspensionRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.UserRole;
import com.backend.orbitflow.domain.user.enums.UserStatus;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 정지 계정은 정지 처리 시점부터 새 로그인과 기존 세션의 모든 서비스 접근을 제한
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SuspensionServiceImpl implements SuspensionService {

    private final SuspensionRepository suspensionRepository;
    private final SuspendedUserCache suspendedUserCache;
    private final TokenService tokenService;
    private final ReportService reportService;

    public SuspensionResponse suspend(User admin, User target, String reason, LocalDateTime expiresAt, Long reportId) {
        if (target.getRole() == UserRole.ROLE_ADMIN) {
            throw new CommonException(SuspensionErrorCode.CANNOT_SUSPEND_ADMIN);
        }
        validateExpiresAt(expiresAt);
        if (suspensionRepository.findActiveByUser(target).isPresent()) {
            throw new CommonException(SuspensionErrorCode.ALREADY_SUSPENDED);
        }
        if (reportId != null) {
            reportService.sanction(reportId, target);
        }

        Suspension suspension = suspensionRepository.save(Suspension.of(target, reason, expiresAt, admin));
        target.updateUserStatus(UserStatus.BANNED);
        afterCommit(() -> {
            // 재발급 차단 + 이미 발급된 access token 차단
            tokenService.deleteRefreshToken(target.getUuid());
            suspendedUserCache.mark(target.getUuid(), expiresAt);
        });
        // TODO: 알림 도메인 구현 후 정지 사실 고지 (이메일 발송 등)
        return SuspensionResponse.from(suspension);
    }

    @Transactional(readOnly = true)
    public Page<SuspensionListResponse> getSuspensions(SuspensionStatus status, String keyword, int page, int size) {
        String trimmed = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return suspensionRepository.search(status, trimmed, PageRequest.of(Math.max(page - 1, 0), size))
                .map(SuspensionListResponse::from);
    }

    @Transactional(readOnly = true)
    public SuspensionResponse getSuspension(Long suspensionId) {
        return SuspensionResponse.from(getSuspensionEntity(suspensionId));
    }

    // 진행 중인 정지의 사유·기간 변경 (기간 변경 즉시 접근 차단 만료 시각 반영)
    public SuspensionResponse updateSuspension(Long suspensionId, String reason, LocalDateTime expiresAt) {
        Suspension suspension = getActiveSuspension(suspensionId);
        validateExpiresAt(expiresAt);
        suspension.updateSuspension(reason, expiresAt);
        String uuid = suspension.getUser().getUuid();
        afterCommit(() -> suspendedUserCache.mark(uuid, expiresAt));
        return SuspensionResponse.from(suspension);
    }

    public SuspensionResponse release(User admin, Long suspensionId, String releasedReason) {
        Suspension suspension = getActiveSuspension(suspensionId);
        suspension.release(admin, releasedReason);
        restoreUser(suspension.getUser());
        // TODO: 알림 도메인 구현 후 정지 해제 고지
        return SuspensionResponse.from(suspension);
    }

    // 일일 배치 : 만료 시각이 지난 기간 정지를 EXPIRED 처리하고 계정 복구
    // 본인의 접근은 Redis 키 만료와 로그인 시 즉시 해제로 지연 없이 복구되며, 배치는 다른 사용자에게 보이는 계정 상태를 정리
    public void expireAll() {
        List<Suspension> expired = suspensionRepository.findAllExpired(LocalDateTime.now());
        expired.forEach(this::expire);
        if (!expired.isEmpty()) {
            log.info("정지 만료 처리 완료: {}건", expired.size());
        }
    }

    // 로그인·소셜 로그인 시 호출 : 만료된 정지는 즉시 해제(일일 배치를 기다리지 않음), 진행 중인 정지만 안내 정보로 반환
    public Optional<SuspendedAccountResponse> findActiveNotice(User user) {
        if (user.getStatus() != UserStatus.BANNED) {
            return Optional.empty();
        }
        Optional<Suspension> active = suspensionRepository.findActiveByUser(user);
        if (active.isEmpty()) {
            // 정지 이력과 계정 상태가 어긋난 경우 계정 상태를 기준 데이터(정지 이력)에 맞춤
            restoreUser(user);
            return Optional.empty();
        }
        Suspension suspension = active.get();
        if (suspension.isExpiredAt(LocalDateTime.now())) {
            expire(suspension);
            return Optional.empty();
        }
        return Optional.of(SuspendedAccountResponse.from(suspension));
    }

    private void expire(Suspension suspension) {
        suspension.expire();
        restoreUser(suspension.getUser());
    }

    private void restoreUser(User user) {
        user.updateUserStatus(UserStatus.ACTIVE);
        String uuid = user.getUuid();
        afterCommit(() -> suspendedUserCache.unmark(uuid));
    }

    private Suspension getSuspensionEntity(Long suspensionId) {
        return suspensionRepository.findWithAllById(suspensionId).orElseThrow(
                () -> new CommonException(SuspensionErrorCode.SUSPENSION_NOT_FOUND)
        );
    }

    private Suspension getActiveSuspension(Long suspensionId) {
        Suspension suspension = getSuspensionEntity(suspensionId);
        if (!suspension.isActive()) {
            throw new CommonException(SuspensionErrorCode.NOT_ACTIVE_SUSPENSION);
        }
        return suspension;
    }

    private void validateExpiresAt(LocalDateTime expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(LocalDateTime.now())) {
            throw new CommonException(SuspensionErrorCode.INVALID_EXPIRES_AT);
        }
    }

    // Redis·토큰 변경은 DB 반영이 확정된 뒤에만 수행
    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
