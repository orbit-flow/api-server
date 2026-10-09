package com.backend.orbitflow.domain.todo.dto.request;

import com.backend.orbitflow.domain.todo.enums.TodoType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TodoCreateRequest(
        @NotNull(message = "투두 유형은 비어있을 수 없습니다.")
        TodoType type,

        @NotBlank(message = "투두 이름은 비어있을 수 없습니다.")
        @Size(max = 100, message = "투두 이름은 100자 이내여야 합니다.")
        String name,

        @NotNull(message = "시작 시각은 비어있을 수 없습니다.")
        LocalDateTime startDate,

        @NotNull(message = "종료 시각은 비어있을 수 없습니다.")
        LocalDateTime endDate,

        // 자식 투두 생성 시 부모 투두 id
        Long parentTodoId,

        // 팀 투두 전용, null이면 생성자
        String assigneeUuid,

        // null이면 리마인드 없음
        @Min(value = 0, message = "리마인드 시간은 0 이상이어야 합니다.")
        Integer remindBeforeMinutes,

        // null이면 반복 없음
        @Valid
        RoutineRequest routine
) {
}
