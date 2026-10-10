package com.backend.orbitflow.domain.todo.facade;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.facade.CategoryFacade;
import com.backend.orbitflow.domain.category.service.CategoryService;
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
import com.backend.orbitflow.domain.todo.entity.Routine;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.RoutineScope;
import com.backend.orbitflow.domain.todo.service.RoutineService;
import com.backend.orbitflow.domain.todo.service.TodoAuthorityService;
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
    private final CategoryService categoryService;
    private final CategoryFacade categoryFacade;
    private final TodoAuthorityService todoAuthorityService;
    private final ApplicationEventPublisher eventPublisher;

    // 팀 투두를 다른 구성원에게 배정해 만들면 담당자에게 알림
    @Transactional
    public TodoResponse createTodo(AuthUser authUser, Long categoryId, TodoCreateRequest request) {
        User me = me(authUser);
        User assignee = request.assigneeUuid() == null ? null : userService.getByUuid(request.assigneeUuid());
        Category category = categoryService.getActiveCategory(categoryId);
        Integer teamMask = categoryFacade.findTeamMask(category, me);
        todoAuthorityService.checkEdit(category, me, teamMask);
        todoService.validatePeriod(request.startDate(), request.endDate());

        Todo parent = null;
        if (request.parentTodoId() != null) {
            parent = activeTodo(request.parentTodoId());
            todoService.validateParent(parent, category, request.routine());
        }

        User todoAssignee = todoService.resolveAssignee(category, me, assignee);
        // 본인에게 배정하는 경우는 정책에서 검사하지 않으므로 구성원 여부를 조회하지 않음
        boolean assigneeMember = !todoAssignee.getId().equals(me.getId()) && categoryFacade.isTeamMember(category, todoAssignee);
        todoAuthorityService.checkAssignOnCreate(category, me, todoAssignee, teamMask, assigneeMember);

        Todo todo = todoService.createTodo(
                category,
                parent,
                todoAssignee,
                request.type(),
                request.name(),
                request.startDate(),
                request.endDate(),
                request.remindBeforeMinutes()
        );
        RoutineRequest routine = request.routine();
        if (routine != null) {
            routineService.upsertRoutine(todo, routine.durationType(), routine.duration(), routine.daysOfWeek(), routine.repeatEndDate(), routine.scope());
        }
        if (category.isTeamCategory()) {
            notifyAssigned(todo, me);
        }
        return todoService.getTodo(todo);
    }

    @Transactional(readOnly = true)
    public PageResponse<TodoResponse> getCategoryTodos(AuthUser authUser, Long categoryId, int page, int size) {
        User me = me(authUser);
        Category category = categoryService.getActiveCategory(categoryId);
        todoAuthorityService.checkView(categoryFacade.canView(category, me));
        return PageResponse.from(todoService.getCategoryTodos(category, page, size));
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodo(AuthUser authUser, Long todoId) {
        User me = me(authUser);
        Todo todo = activeTodo(todoId);
        todoAuthorityService.checkView(categoryFacade.canView(todo.getCategory(), me));
        return todoService.getTodo(todo);
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
        return PageResponse.of(routineService.getDashboardPreviews(me(authUser), dateOrToday(date)), page, size);
    }

    private LocalDate dateOrToday(LocalDate date) {
        return date == null ? LocalDate.now() : date;
    }

    @Transactional
    public TodoResponse updateTodo(AuthUser authUser, Long todoId, TodoUpdateRequest request) {
        User me = me(authUser);
        Todo todo = activeTodo(todoId);
        todoAuthorityService.checkEdit(todo.getCategory(), me, categoryFacade.findTeamMask(todo.getCategory(), me));
        return todoService.updateTodo(
                todo,
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
        Todo todo = activeTodo(todoId);
        todoAuthorityService.checkComplete(todo, me, categoryFacade.findTeamMask(todo.getCategory(), me));
        TodoResponse response = todoService.toggleComplete(todo);
        if (response.isCompleted()) {
            categoryFacade.findNotifiableViewers(todo.getCategory(), me).forEach(receivers ->
                    eventPublisher.publishEvent(new NotificationRequest(receivers, NotificationType.TODO_COMPLETED, me, todo.getId(), null,
                            me.getName() + "님이 '" + todo.getName() + "'을(를) 완료했습니다.")));
        }
        return response;
    }

    // 담당자가 바뀌면 새 담당자에게 알림
    @Transactional
    public TodoResponse updateAssignee(AuthUser authUser, Long todoId, TodoAssigneeRequest request) {
        User me = me(authUser);
        User assignee = userService.getByUuid(request.userUuid());
        Todo todo = activeTodo(todoId);
        todoService.requireTeamTodo(todo);
        Category category = todo.getCategory();
        todoAuthorityService.checkAssign(categoryFacade.findTeamMask(category, me), categoryFacade.isTeamMember(category, assignee));
        Long previousAssigneeId = todo.getAssignee().getId();
        TodoResponse response = todoService.updateAssignee(todo, assignee);
        if (!todo.getAssignee().getId().equals(previousAssigneeId)) {
            notifyAssigned(todo, me);
        }
        return response;
    }

    @Transactional
    public void updateChildOrder(AuthUser authUser, Long todoId, TodoOrderRequest request) {
        User me = me(authUser);
        Todo parent = activeTodo(todoId);
        todoAuthorityService.checkEdit(parent.getCategory(), me, categoryFacade.findTeamMask(parent.getCategory(), me));
        todoService.updateChildOrder(parent, request.todoIds());
    }

    @Transactional
    public void deleteTodo(AuthUser authUser, Long todoId) {
        User me = me(authUser);
        Todo todo = activeTodo(todoId);
        todoAuthorityService.checkEdit(todo.getCategory(), me, categoryFacade.findTeamMask(todo.getCategory(), me));
        todoService.deleteTodo(todo);
    }

    @Transactional(readOnly = true)
    public PageResponse<TodoResponse> getRestorableTodos(AuthUser authUser, Long categoryId, int page, int size) {
        User me = me(authUser);
        Category category = categoryService.getActiveCategory(categoryId);
        todoAuthorityService.checkEdit(category, me, categoryFacade.findTeamMask(category, me));
        return PageResponse.from(todoService.getRestorableTodos(category, page, size));
    }

    @Transactional
    public TodoResponse restoreTodo(AuthUser authUser, Long todoId) {
        User me = me(authUser);
        Todo todo = todoService.getRestorableTodo(todoId);
        Category category = categoryService.getActiveCategory(todo.getCategory().getId());
        todoAuthorityService.checkEdit(category, me, categoryFacade.findTeamMask(category, me));
        return todoService.restoreTodo(todo);
    }

    @Transactional
    public RoutineResponse upsertRoutine(AuthUser authUser, Long todoId, RoutineRequest request) {
        // 반복 설정은 반복에 속한 투두(원본·회차) 어디서든 가능, 반복 중이면 변경 범위(scope)를 함께 받음
        User me = me(authUser);
        Todo todo = activeTodo(todoId);
        todoAuthorityService.checkEdit(todo.getCategory(), me, categoryFacade.findTeamMask(todo.getCategory(), me));
        return routineService.upsertRoutine(todo, request.durationType(), request.duration(), request.daysOfWeek(), request.repeatEndDate(), request.scope());
    }

    @Transactional
    public void deleteRoutine(AuthUser authUser, Long todoId, RoutineScope scope) {
        User me = me(authUser);
        Todo todo = activeTodo(todoId);
        todoAuthorityService.checkEdit(todo.getCategory(), me, categoryFacade.findTeamMask(todo.getCategory(), me));
        routineService.deleteRoutine(todo, scope);
    }

    @Transactional
    public TodoResponse createOccurrence(AuthUser authUser, Long routineId, RoutineOccurrenceRequest request) {
        User me = me(authUser);
        Routine routine = routineService.getRoutine(routineId);
        Category category = routine.getTodo().getCategory();
        todoAuthorityService.checkEdit(category, me, categoryFacade.findTeamMask(category, me));
        return routineService.createOccurrence(routine, request.startDate());
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

    // 소유자가 탈퇴했거나 팀이 삭제된 카테고리의 투두도 존재하지 않는 것으로 처리
    private Todo activeTodo(Long todoId) {
        Todo todo = todoService.getActiveTodo(todoId);
        categoryService.getActiveCategory(todo.getCategory().getId());
        return todo;
    }

    private User me(AuthUser authUser) {
        return userService.getActiveByUuid(authUser.getUuid());
    }
}
