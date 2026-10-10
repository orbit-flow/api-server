package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.todo.dto.request.RoutineRequest;
import com.backend.orbitflow.domain.todo.dto.response.RoutineResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.RoutineScope;
import com.backend.orbitflow.domain.todo.enums.TodoType;
import com.backend.orbitflow.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.backend.orbitflow.domain.todo.dto.response.DashboardTodoResponse;
import org.springframework.data.domain.Page;

public interface TodoService {

    int RETENTION_DAYS = 30;

    TodoResponse createTodo(
            User actor, Long categoryId, Long parentTodoId, User assignee, TodoType type, String name,
            LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes, RoutineRequest routine
    );
    Page<TodoResponse> getCategoryTodos(User viewer, Long categoryId, int page, int size);
    TodoResponse getTodo(User viewer, Long todoId);
    Page<DashboardTodoResponse> getDashboardSchedules(User me, LocalDate date, int page, int size);
    Page<DashboardTodoResponse> getDashboardBacklogs(User me, LocalDate date, int page, int size);
    List<DashboardTodoResponse> getDashboardPreviews(User me, LocalDate date);
    TodoResponse updateTodo(User actor, Long todoId, TodoType type, String name, LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes);
    TodoResponse toggleComplete(User actor, Long todoId);
    TodoResponse updateAssignee(User actor, Long todoId, User assignee);
    void updateChildOrder(User actor, Long parentTodoId, List<Long> childTodoIds);
    void deleteTodo(User actor, Long todoId);
    Page<TodoResponse> getRestorableTodos(User actor, Long categoryId, int page, int size);
    TodoResponse restoreTodo(User actor, Long todoId);
    RoutineResponse upsertRoutine(User actor, Long todoId, RoutineRequest request);
    void deleteRoutine(User actor, Long todoId, RoutineScope scope);

    // 다른 도메인에서 사용 : 논리적 삭제된 투두 포함 (카테고리·담당자와 함께 조회)
    Optional<Todo> findTodo(Long todoId);
}
