package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.todo.dto.request.RoutineRequest;
import com.backend.orbitflow.domain.todo.dto.response.DashboardResponse;
import com.backend.orbitflow.domain.todo.dto.response.RoutineResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.enums.TodoType;
import com.backend.orbitflow.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TodoService {

    int RETENTION_DAYS = 30;

    TodoResponse createTodo(
            User actor, Long categoryId, Long parentTodoId, User assignee, TodoType type, String name,
            LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes, RoutineRequest routine
    );
    List<TodoResponse> getCategoryTodos(User viewer, Long categoryId);
    TodoResponse getTodo(User viewer, Long todoId);
    DashboardResponse getDashboard(User me, LocalDate date);
    TodoResponse updateTodo(User actor, Long todoId, TodoType type, String name, LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes);
    TodoResponse toggleComplete(User actor, Long todoId);
    TodoResponse updateAssignee(User actor, Long todoId, User assignee);
    List<TodoResponse> updateChildOrder(User actor, Long parentTodoId, List<Long> childTodoIds);
    void deleteTodo(User actor, Long todoId);
    List<TodoResponse> getRestorableTodos(User actor, Long categoryId);
    TodoResponse restoreTodo(User actor, Long todoId);
    RoutineResponse upsertRoutine(User actor, Long todoId, RoutineRequest request);
    void deleteRoutine(User actor, Long todoId);
}
