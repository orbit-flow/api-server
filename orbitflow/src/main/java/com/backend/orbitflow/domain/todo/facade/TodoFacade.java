package com.backend.orbitflow.domain.todo.facade;

import com.backend.orbitflow.domain.todo.dto.request.RoutineOccurrenceRequest;
import com.backend.orbitflow.domain.todo.dto.request.RoutineRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoAssigneeRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoCreateRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoOrderRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoUpdateRequest;
import com.backend.orbitflow.domain.todo.dto.response.DashboardResponse;
import com.backend.orbitflow.domain.todo.dto.response.RoutineResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.service.RoutineService;
import com.backend.orbitflow.domain.todo.service.TodoService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TodoFacade {

    private final TodoService todoService;
    private final RoutineService routineService;
    private final UserService userService;

    @Transactional
    public TodoResponse createTodo(AuthUser authUser, Long categoryId, TodoCreateRequest request) {
        User assignee = request.assigneeUuid() == null ? null : userService.getByUuid(request.assigneeUuid());
        return todoService.createTodo(
                me(authUser),
                categoryId,
                request.parentTodoId(),
                assignee,
                request.type(),
                request.name(),
                request.startDate(),
                request.endDate(),
                request.remindBeforeMinutes(),
                request.routine()
        );
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> getCategoryTodos(AuthUser authUser, Long categoryId) {
        return todoService.getCategoryTodos(me(authUser), categoryId);
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodo(AuthUser authUser, Long todoId) {
        return todoService.getTodo(me(authUser), todoId);
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(AuthUser authUser, LocalDate date) {
        return todoService.getDashboard(me(authUser), date == null ? LocalDate.now() : date);
    }

    @Transactional
    public TodoResponse updateTodo(AuthUser authUser, Long todoId, TodoUpdateRequest request) {
        return todoService.updateTodo(
                me(authUser),
                todoId,
                request.type(),
                request.name(),
                request.startDate(),
                request.endDate(),
                request.remindBeforeMinutes()
        );
    }

    @Transactional
    public TodoResponse toggleComplete(AuthUser authUser, Long todoId) {
        return todoService.toggleComplete(me(authUser), todoId);
    }

    @Transactional
    public TodoResponse updateAssignee(AuthUser authUser, Long todoId, TodoAssigneeRequest request) {
        return todoService.updateAssignee(me(authUser), todoId, userService.getByUuid(request.userUuid()));
    }

    @Transactional
    public List<TodoResponse> updateChildOrder(AuthUser authUser, Long todoId, TodoOrderRequest request) {
        return todoService.updateChildOrder(me(authUser), todoId, request.todoIds());
    }

    @Transactional
    public void deleteTodo(AuthUser authUser, Long todoId) {
        todoService.deleteTodo(me(authUser), todoId);
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> getRestorableTodos(AuthUser authUser, Long categoryId) {
        return todoService.getRestorableTodos(me(authUser), categoryId);
    }

    @Transactional
    public TodoResponse restoreTodo(AuthUser authUser, Long todoId) {
        return todoService.restoreTodo(me(authUser), todoId);
    }

    @Transactional
    public RoutineResponse upsertRoutine(AuthUser authUser, Long todoId, RoutineRequest request) {
        return todoService.upsertRoutine(me(authUser), todoId, request);
    }

    @Transactional
    public void deleteRoutine(AuthUser authUser, Long todoId) {
        todoService.deleteRoutine(me(authUser), todoId);
    }

    @Transactional
    public TodoResponse createOccurrence(AuthUser authUser, Long routineId, RoutineOccurrenceRequest request) {
        return routineService.createOccurrence(me(authUser), routineId, request.startDate());
    }

    private User me(AuthUser authUser) {
        return userService.getByUuid(authUser.getUuid());
    }
}
