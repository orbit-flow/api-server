package com.backend.orbitflow.domain.point.service;

import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.avatar.service.AvatarService;
import com.backend.orbitflow.domain.notification.event.PointEarnedEvent;
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
    private final AvatarService avatarService;
    private final ApplicationEventPublisher eventPublisher;

    // 운영일 기준 하루 1회, 요청 성공 즉시 잔액 반영 (아바타 행 락으로 동시 출석 요청 차단)
    public PointTransactionResponse attend(User me) {
        Avatar avatar = avatarService.getForUpdate(me);
        if (attendedToday(me)) {
            throw new CommonException(PointErrorCode.ALREADY_ATTENDED);
        }
        avatar.addPoint(ATTENDANCE_POINT);
        PointTransaction transaction = pointTransactionRepository.save(PointTransaction.of(
                me, PointTransactionType.ATTENDANCE, ATTENDANCE_POINT, avatar.getPoint()
        ));
        eventPublisher.publishEvent(new PointEarnedEvent(me.getId(), ATTENDANCE_POINT, avatar.getPoint()));
        return PointTransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public AttendanceStatusResponse getAttendanceStatus(User me) {
        return new AttendanceStatusResponse(LocalDate.now(SERVICE_ZONE), attendedToday(me));
    }

    @Transactional(readOnly = true)
    public Page<PointTransactionResponse> getHistory(User me, int page, int size) {
        return pointTransactionRepository.findAllByUserOrderByCreatedAtDescIdDesc(me, PageRequest.of(Math.max(page - 1, 0), size))
                .map(PointTransactionResponse::from);
    }

    // created_at은 서버 기본 시간대로 저장되므로 운영일(Asia/Seoul) 경계를 서버 시간대로 변환해 비교
    private boolean attendedToday(User me) {
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        LocalDateTime from = toServerTime(today);
        LocalDateTime to = toServerTime(today.plusDays(1));
        return pointTransactionRepository.existsByUserAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                me, PointTransactionType.ATTENDANCE, from, to
        );
    }

    private LocalDateTime toServerTime(LocalDate serviceDate) {
        return serviceDate.atStartOfDay(SERVICE_ZONE)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
