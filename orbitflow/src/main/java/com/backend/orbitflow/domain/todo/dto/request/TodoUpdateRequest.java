package com.backend.orbitflow.domain.todo.dto.request;

import com.backend.orbitflow.domain.todo.enums.TodoType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TodoUpdateRequest(
        @NotNull(message = "투두 유형은 비어있을 수 없습니다.")
        TodoType type,

        @NotBlank(message = "투두 이름은 비어있을 수 없습니다.")
        @Size(max = 100, message = "투두 이름은 100자 이내여야 합니다.")
        String name,

        @NotNull(message = "시작 시각은 비어있을 수 없습니다.")
        LocalDateTime startDate,

        @NotNull(message = "종료 시각은 비어있을 수 없습니다.")
        LocalDateTime endDate,

        @Min(value = 0, message = "리마인드 시간은 0 이상이어야 합니다.")
        Integer remindBeforeMinutes
) {
}
