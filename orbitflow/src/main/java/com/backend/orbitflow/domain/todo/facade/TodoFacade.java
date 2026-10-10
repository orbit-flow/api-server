package com.backend.orbitflow.domain.todo.facade;

import com.backend.orbitflow.domain.category.service.CategoryAuthorityService;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.todo.dto.request.RoutineOccurrenceRequest;
import com.backend.orbitflow.domain.todo.dto.request.RoutineRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoAssigneeRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoCreateRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoOrderRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoUpdateRequest;
import com.backend.orbitflow.domain.todo.dto.response.RoutineResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.RoutineScope;
import com.backend.orbitflow.domain.todo.service.RoutineService;
import com.backend.orbitflow.domain.todo.service.TodoService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import com.backend.orbitflow.domain.todo.dto.response.DashboardTodoResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;

@Component
@RequiredArgsConstructor
public class TodoFacade {

    private final TodoService todoService;
    private final RoutineService routineService;
    private final UserService userService;
    private final CategoryAuthorityService categoryAuthorityService;
    private final ApplicationEventPublisher eventPublisher;

    // 팀 투두를 다른 구성원에게 배정해 만들면 담당자에게 알림
    @Transactional
    public TodoResponse createTodo(AuthUser authUser, Long categoryId, TodoCreateRequest request) {
        User me = me(authUser);
        User assignee = request.assigneeUuid() == null ? null : userService.getByUuid(request.assigneeUuid());
        TodoResponse response = todoService.createTodo(
                me,
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
        Todo todo = todoService.findTodo(response.id()).orElseThrow();
        if (todo.getCategory().isTeamCategory()) {
            notifyAssigned(todo, me);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<TodoResponse> getCategoryTodos(AuthUser authUser, Long categoryId, int page, int size) {
        return PageResponse.from(todoService.getCategoryTodos(me(authUser), categoryId, page, size));
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodo(AuthUser authUser, Long todoId) {
        return todoService.getTodo(me(authUser), todoId);
    }

    @Transactional(readOnly = true)
    public PageResponse<DashboardTodoResponse> getDashboardSchedules(AuthUser authUser, LocalDate date, int page, int size) {
        return PageResponse.from(todoService.getDashboardSchedules(me(authUser), dateOrToday(date), page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<DashboardTodoResponse> getDashboardBacklogs(AuthUser authUser, LocalDate date, int page, int size) {
        return PageResponse.from(todoService.getDashboardBacklogs(me(authUser), dateOrToday(date), page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<DashboardTodoResponse> getDashboardPreviews(AuthUser authUser, LocalDate date, int page, int size) {
        return PageResponse.of(todoService.getDashboardPreviews(me(authUser), dateOrToday(date)), page, size);
    }

    private LocalDate dateOrToday(LocalDate date) {
        return date == null ? LocalDate.now() : date;
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

    // 완료 처리 시 알림을 켠 팔로워 중 투두를 조회할 수 있는 사용자에게 알림 (완료 취소는 알림 없음)
    @Transactional
    public TodoResponse toggleComplete(AuthUser authUser, Long todoId) {
        User me = me(authUser);
        TodoResponse response = todoService.toggleComplete(me, todoId);
        if (response.isCompleted()) {
            Todo todo = todoService.findTodo(todoId).orElseThrow();
            categoryAuthorityService.findNotifiableViewers(todo.getCategory(), me).forEach(receivers ->
                    eventPublisher.publishEvent(new NotificationRequest(receivers, NotificationType.TODO_COMPLETED, me, todo.getId(), null,
                            me.getName() + "님이 '" + todo.getName() + "'을(를) 완료했습니다.")));
        }
        return response;
    }

    // 담당자가 바뀌면 새 담당자에게 알림
    @Transactional
    public TodoResponse updateAssignee(AuthUser authUser, Long todoId, TodoAssigneeRequest request) {
        User me = me(authUser);
        Long previousAssigneeId = todoService.findTodo(todoId).map(todo -> todo.getAssignee().getId()).orElse(null);
        TodoResponse response = todoService.updateAssignee(me, todoId, userService.getByUuid(request.userUuid()));
        Todo todo = todoService.findTodo(todoId).orElseThrow();
        if (!todo.getAssignee().getId().equals(previousAssigneeId)) {
            notifyAssigned(todo, me);
        }
        return response;
    }

    @Transactional
    public void updateChildOrder(AuthUser authUser, Long todoId, TodoOrderRequest request) {
        todoService.updateChildOrder(me(authUser), todoId, request.todoIds());
    }

    @Transactional
    public void deleteTodo(AuthUser authUser, Long todoId) {
        todoService.deleteTodo(me(authUser), todoId);
    }

    @Transactional(readOnly = true)
    public PageResponse<TodoResponse> getRestorableTodos(AuthUser authUser, Long categoryId, int page, int size) {
        return PageResponse.from(todoService.getRestorableTodos(me(authUser), categoryId, page, size));
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
    public void deleteRoutine(AuthUser authUser, Long todoId, RoutineScope scope) {
        todoService.deleteRoutine(me(authUser), todoId, scope);
    }

    @Transactional
    public TodoResponse createOccurrence(AuthUser authUser, Long routineId, RoutineOccurrenceRequest request) {
        return routineService.createOccurrence(me(authUser), routineId, request.startDate());
    }

    // 팀 투두 배정 : 본인에게 배정한 경우는 제외
    private void notifyAssigned(Todo todo, User actor) {
        User assignee = todo.getAssignee();
        if (assignee.getId().equals(actor.getId())) {
            return;
        }
        eventPublisher.publishEvent(NotificationRequest.to(assignee, NotificationType.TODO_ASSIGNED, actor, todo.getId(), null,
                actor.getName() + "님이 '" + todo.getName() + "'을(를) 회원님에게 배정했습니다."));
    }

    private User me(AuthUser authUser) {
        return userService.getByUuid(authUser.getUuid());
    }
}
