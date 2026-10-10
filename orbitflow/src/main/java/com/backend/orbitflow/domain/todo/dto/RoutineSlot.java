package com.backend.orbitflow.domain.todo.dto;

import java.time.LocalDateTime;

// 반복 규칙별 이미 생성된 회차의 슬롯(원래 시각) 한 줄 (여러 반복 규칙을 한 번에 조회)
public record RoutineSlot(Long routineId, LocalDateTime slot) {
}
