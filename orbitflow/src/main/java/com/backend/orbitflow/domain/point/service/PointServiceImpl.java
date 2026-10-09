package com.backend.orbitflow.domain.point.service;

import com.backend.orbitflow.domain.avatar.dto.response.LevelResponse;
import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.avatar.policy.LevelPolicy;
import com.backend.orbitflow.domain.notification.event.PointEarnedEvent;
import com.backend.orbitflow.domain.notification.event.PointRevokedEvent;
import com.backend.orbitflow.domain.point.dto.response.AttendanceResponse;
import com.backend.orbitflow.domain.point.dto.response.AttendanceStatusResponse;
import com.backend.orbitflow.domain.point.dto.response.PointTransactionResponse;
import com.backend.orbitflow.domain.point.entity.PointTransaction;
import com.backend.orbitflow.domain.point.enums.PointTransactionType;
import com.backend.orbitflow.domain.point.error.PointErrorCode;
import com.backend.orbitflow.domain.point.repository.PointTransactionRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional
public class PointServiceImpl implements PointService {

    // 서비스 운영일 기준 시간대
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final PointTransactionRepository pointTransactionRepository;
    private final PointLedger pointLedger;
    private final ApplicationEventPublisher eventPublisher;

    // 운영일 기준 하루 1회 : 아바타 행 락을 먼저 잡은 뒤 오늘 출석 여부를 확인하므로
    // 동시 출석 요청은 직렬화되어 두 번째 요청은 첫 요청의 출석 기록을 보고 거부됨
    @Transactional(isolation = Isolation.READ_COMMITTED)
    // 출석은 경험치를 얻는 유일한 경로 : 포인트와 같은 아바타 락 아래에서 경험치·레벨 반영
    public AttendanceResponse attend(User me) {
        Avatar avatar = pointLedger.lock(me);
        if (attendedToday(me)) {
            throw new CommonException(PointErrorCode.ALREADY_ATTENDED);
        }
        PointTransaction transaction = pointLedger.deposit(me, PointTransactionType.ATTENDANCE, ATTENDANCE_POINT);
        int before = avatar.getLevel();
        avatar.gainExp(LevelPolicy.ATTENDANCE_EXP);
        eventPublisher.publishEvent(new PointEarnedEvent(me.getId(), "출석 포인트", ATTENDANCE_POINT, transaction.getBalanceAfter()));
        return new AttendanceResponse(
                PointTransactionResponse.from(transaction),
                LevelPolicy.ATTENDANCE_EXP,
                avatar.getLevel() > before,
                LevelResponse.from(avatar)
        );
    }

    @Transactional(readOnly = true)
    public AttendanceStatusResponse getAttendanceStatus(User me) {
        return new AttendanceStatusResponse(LocalDate.now(SERVICE_ZONE), attendedToday(me));
    }

    @Transactional(readOnly = true)
    public Page<PointTransactionResponse> getHistory(User me, int page, int size) {
        return getUserHistory(me, null, page, size);
    }

    @Transactional(readOnly = true)
    public Page<PointTransactionResponse> getUserHistory(User user, PointTransactionType type, int page, int size) {
        return pointTransactionRepository.search(user, type, PageRequest.of(Math.max(page - 1, 0), size))
                .map(PointTransactionResponse::from);
    }

    // 무효·부정 출석으로 확정된 적립 포인트 회수 (잔액이 부족하면 음수로 기록)
    // 해당 출석으로 얻은 경험치도 차감 (레벨 하락 가능)
    // 중복 회수 방지 : 아바타 락 아래에서 회수 여부 확인 + source_transaction_id unique 제약
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PointTransactionResponse revokeAttendance(Long transactionId) {
        PointTransaction source = pointTransactionRepository.findWithUserById(transactionId).orElseThrow(
                () -> new CommonException(PointErrorCode.TRANSACTION_NOT_FOUND)
        );
        if (source.getType() != PointTransactionType.ATTENDANCE) {
            throw new CommonException(PointErrorCode.NOT_REVOCABLE_TRANSACTION);
        }
        User user = source.getUser();
        Avatar avatar = pointLedger.lock(user);
        if (pointTransactionRepository.existsBySourceTransaction(source)) {
            throw new CommonException(PointErrorCode.ALREADY_REVOKED);
        }
        PointTransaction revoke = pointLedger.revoke(user, source);
        avatar.loseExp(LevelPolicy.ATTENDANCE_EXP);
        eventPublisher.publishEvent(new PointRevokedEvent(user.getId(), source.getAmount(), revoke.getBalanceAfter()));
        return PointTransactionResponse.from(revoke);
    }

    // created_at은 서버 기본 시간대로 저장되므로 운영일(Asia/Seoul) 경계를 서버 시간대로 변환해 비교
    private boolean attendedToday(User me) {
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        return pointTransactionRepository.existsByUserAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                me, PointTransactionType.ATTENDANCE, toServerTime(today), toServerTime(today.plusDays(1))
        );
    }

    private LocalDateTime toServerTime(LocalDate serviceDate) {
        return serviceDate.atStartOfDay(SERVICE_ZONE)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
