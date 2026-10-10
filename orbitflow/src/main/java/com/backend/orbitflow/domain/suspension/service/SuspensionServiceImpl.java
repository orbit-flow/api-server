package com.backend.orbitflow.domain.suspension.service;

import com.backend.orbitflow.domain.auth.service.TokenService;
import com.backend.orbitflow.domain.suspension.event.UserSuspendedEvent;
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
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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

    // 일괄 UPDATE의 IN 목록 최대 크기
    private static final int BULK_CHUNK_SIZE = 500;

    private final SuspensionRepository suspensionRepository;
    private final SuspendedUserCache suspendedUserCache;
    private final TokenService tokenService;
    private final ReportService reportService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    // 대상 사용자 행 락 아래에서 진행 중인 정지를 확인하므로 동시 정지 요청이 직렬화됨 (READ COMMITTED 필수 : 락 이후 조회가 앞선 커밋을 봐야 함)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SuspensionResponse suspend(User admin, User target, String reason, LocalDateTime expiresAt, Long reportId) {
        userService.lockUser(target.getId());
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
        eventPublisher.publishEvent(new UserSuspendedEvent(target.getUuid()));
        // TODO: 알림 도메인 구현 후 정지 사실 고지 (이메일 발송 등)
        return SuspensionResponse.from(suspension);
    }

    @Transactional(readOnly = true)
    public Page<SuspensionListResponse> getSuspensions(SuspensionStatus status, String keyword, int page, int size) {
        String trimmed = keyword == null || keyword.isBlank() ? null : escapeLike(keyword.trim());
        return suspensionRepository.search(status, trimmed, PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100)));
    }

    // LIKE 와일드카드(%, _)와 이스케이프 문자(!)를 문자 그대로 검색하도록 이스케이프 (쿼리의 escape '!'와 짝)
    // 백슬래시는 MySQL 문자열 리터럴 이스케이프와 겹치므로 이스케이프 문자로 쓰지 않음
    private String escapeLike(String keyword) {
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
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
    // 건별 UPDATE 대신 IN 목록 일괄 UPDATE (청크 단위), Redis 정지 표시는 커밋 후 다중 키 삭제 1회
    public void expireAll() {
        LocalDateTime now = LocalDateTime.now();
        List<SuspensionRepository.ExpiredSuspension> expired = suspensionRepository.findAllExpired(now);
        if (expired.isEmpty()) {
            return;
        }
        for (int from = 0; from < expired.size(); from += BULK_CHUNK_SIZE) {
            List<SuspensionRepository.ExpiredSuspension> chunk = expired.subList(from, Math.min(from + BULK_CHUNK_SIZE, expired.size()));
            suspensionRepository.expireAllByIdIn(chunk.stream().map(SuspensionRepository.ExpiredSuspension::getId).toList(), now);
            suspensionRepository.restoreUsersByIdIn(chunk.stream().map(SuspensionRepository.ExpiredSuspension::getUserId).toList(), now);
        }
        List<String> uuids = expired.stream().map(SuspensionRepository.ExpiredSuspension::getUserUuid).toList();
        afterCommit(() -> suspendedUserCache.unmarkAll(uuids));
        log.info("정지 만료 처리 완료: {}건", expired.size());
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
