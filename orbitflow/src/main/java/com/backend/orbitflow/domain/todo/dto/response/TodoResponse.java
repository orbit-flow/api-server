package com.backend.orbitflow.domain.todo.dto.response;

import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.TodoType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record TodoResponse(
        Long id,
        Long categoryId,
        Long parentTodoId,
        Long routineId,
        TodoType type,
        String name,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startDate,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime endDate,
        boolean isCompleted,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime completedAt,
        Integer remindBeforeMinutes,
        int sortOrder,
        String assigneeUuid,
        String assigneeName,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime deletedAt
) {

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getCategory().getId(),
                todo.getParentTodo() == null ? null : todo.getParentTodo().getId(),
                todo.getRoutine() == null ? null : todo.getRoutine().getId(),
                todo.getType(),
                todo.getName(),
                todo.getStartDate(),
                todo.getEndDate(),
                todo.isCompleted(),
                todo.getCompletedAt(),
                todo.getRemindBeforeMinutes(),
                todo.getSortOrder(),
                todo.getAssignee().getUuid(),
                todo.getAssignee().getName(),
                todo.getDeletedAt()
        );
    }
}
