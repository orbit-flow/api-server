package com.backend.orbitflow.domain.todo.dto.response;

import com.backend.orbitflow.domain.todo.entity.Routine;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.TodoType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Duration;
import java.time.LocalDateTime;

// 아직 생성되지 않은 반복 회차 (작업 요청 시 POST /api/routines/{routineId}/occurrences 로 생성)
public record TodoPreviewResponse(
        Long routineId,
        Long categoryId,
        TodoType type,
        String name,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startDate,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime endDate
) {

    public static TodoPreviewResponse of(Routine routine, LocalDateTime startDate) {
        Todo origin = routine.getTodo();
        return new TodoPreviewResponse(
                routine.getId(),
                origin.getCategory().getId(),
                origin.getType(),
                origin.getName(),
                startDate,
                startDate.plus(Duration.between(origin.getStartDate(), origin.getEndDate()))
        );
    }
}
