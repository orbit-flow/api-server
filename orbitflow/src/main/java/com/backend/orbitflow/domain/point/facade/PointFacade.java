package com.backend.orbitflow.domain.point.facade;

import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.point.dto.response.AttendanceResponse;
import com.backend.orbitflow.domain.point.dto.response.AttendanceStatusResponse;
import com.backend.orbitflow.domain.point.dto.response.PointTransactionResponse;
import com.backend.orbitflow.domain.point.enums.PointTransactionType;
import com.backend.orbitflow.domain.point.service.PointService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PointFacade {

    private final PointService pointService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    // 포인트 변경 : READ COMMITTED 필수 (PointLedger 참고), 적립 결과는 처리 완료 즉시 앱 내 알림으로 고지
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AttendanceResponse attend(AuthUser authUser) {
        User me = userService.getByUuid(authUser.getUuid());
        AttendanceResponse response = pointService.attend(me);
        eventPublisher.publishEvent(NotificationRequest.to(me, NotificationType.POINT_EARNED, null, null, null,
                "출석 포인트 " + response.transaction().amount() + "P가 적립되었습니다. (잔액 " + response.transaction().balanceAfter() + "P)"));
        return response;
    }

    @Transactional(readOnly = true)
    public AttendanceStatusResponse getAttendanceStatus(AuthUser authUser) {
        return pointService.getAttendanceStatus(userService.getByUuid(authUser.getUuid()));
    }

    @Transactional(readOnly = true)
    public PageResponse<PointTransactionResponse> getHistory(AuthUser authUser, int page, int size) {
        return PageResponse.from(pointService.getHistory(userService.getByUuid(authUser.getUuid()), page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<PointTransactionResponse> getUserHistory(String userUuid, PointTransactionType type, int page, int size) {
        return PageResponse.from(pointService.getUserHistory(userService.getByUuidIncludingBanned(userUuid), type, page, size));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PointTransactionResponse revokeAttendance(Long transactionId) {
        return pointService.revokeAttendance(transactionId);
    }
}
