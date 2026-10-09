package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.todo.dto.response.RoutineResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoPreviewResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.DurationType;
import com.backend.orbitflow.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public interface RoutineService {

    // 각 회차는 시작일 1개월 전에 미리 생성
    int GENERATION_MONTHS = 1;

    RoutineResponse upsertRoutine(Todo todo, DurationType durationType, int duration, Integer daysOfWeek, LocalDateTime repeatEndDate);
    void deleteRoutine(Todo todo);
    TodoResponse createOccurrence(User actor, Long routineId, LocalDateTime startDate);
    List<TodoPreviewResponse> getPreviews(User user, LocalDateTime from, LocalDateTime to);
    void generateAll();
}
