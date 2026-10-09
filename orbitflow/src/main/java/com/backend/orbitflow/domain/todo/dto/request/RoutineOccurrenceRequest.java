package com.backend.orbitflow.domain.todo.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// 미리보기 회차의 시작 시각
public record RoutineOccurrenceRequest(
        @NotNull(message = "회차 시작 시각은 비어있을 수 없습니다.")
        LocalDateTime startDate
) {
}
