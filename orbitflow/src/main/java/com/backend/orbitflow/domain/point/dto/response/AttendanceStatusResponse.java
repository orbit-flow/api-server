package com.backend.orbitflow.domain.point.dto.response;

import java.time.LocalDate;

// 서비스 운영일(Asia/Seoul) 기준 오늘 출석 여부
public record AttendanceStatusResponse(
        LocalDate date,
        boolean attended
) {
}
