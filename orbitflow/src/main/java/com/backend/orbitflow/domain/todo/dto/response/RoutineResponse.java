package com.backend.orbitflow.domain.todo.dto.response;

import com.backend.orbitflow.domain.todo.entity.Routine;
import com.backend.orbitflow.domain.todo.enums.DurationType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record RoutineResponse(
        Long id,
        Long originTodoId,
        DurationType durationType,
        int duration,
        Integer daysOfWeek,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime repeatEndDate
) {

    public static RoutineResponse from(Routine routine) {
        return new RoutineResponse(
                routine.getId(),
                routine.getTodo().getId(),
                routine.getDurationType(),
                routine.getDuration(),
                routine.getDaysOfWeek(),
                routine.getRepeatEndDate()
        );
    }
}
