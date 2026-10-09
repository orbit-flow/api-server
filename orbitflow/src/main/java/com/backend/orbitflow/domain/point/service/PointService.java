package com.backend.orbitflow.domain.point.service;

import com.backend.orbitflow.domain.point.dto.response.AttendanceStatusResponse;
import com.backend.orbitflow.domain.point.dto.response.PointTransactionResponse;
import com.backend.orbitflow.domain.point.enums.PointTransactionType;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;

public interface PointService {

    // 유효한 출석 1회당 1포인트
    int ATTENDANCE_POINT = 1;

    PointTransactionResponse attend(User me);
    AttendanceStatusResponse getAttendanceStatus(User me);
    Page<PointTransactionResponse> getHistory(User me, int page, int size);

    // 관리자
    Page<PointTransactionResponse> getUserHistory(User user, PointTransactionType type, int page, int size);
    PointTransactionResponse revokeAttendance(Long transactionId);
}
