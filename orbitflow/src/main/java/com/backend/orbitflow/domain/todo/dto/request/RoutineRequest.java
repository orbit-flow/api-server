package com.backend.orbitflow.domain.todo.dto.request;

import com.backend.orbitflow.domain.todo.enums.DurationType;
import com.backend.orbitflow.domain.todo.enums.RoutineScope;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// 매주 : WEEK/1/요일, 격주 : WEEK/2/요일, 매월 : MONTH/1, 말일 : MONTH_END/1, 사용자 지정 : DAY/n 등
public record RoutineRequest(
        @NotNull(message = "반복 단위는 비어있을 수 없습니다.")
        DurationType durationType,

        @Min(value = 1, message = "반복 간격은 1 이상이어야 합니다.")
        @Max(value = 365, message = "반복 간격은 365 이하여야 합니다.")
        int duration,

        // WEEK 전용 요일 비트마스크 (MON=1 ~ SUN=64), null이면 원본 투두의 요일
        Integer daysOfWeek,

        // null이면 종료일 없음
        LocalDateTime repeatEndDate,

        // 이미 반복 중인 투두의 규칙을 변경할 때 필수 (ALL : 전체, FROM_TODAY : 오늘부터), 새로 설정할 때는 무시
        RoutineScope scope
) {
}
