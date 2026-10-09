package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.service.CategoryService;
import com.backend.orbitflow.domain.todo.dto.request.RoutineRequest;
import com.backend.orbitflow.domain.todo.dto.response.DashboardResponse;
import com.backend.orbitflow.domain.todo.dto.response.RoutineResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.TodoType;
import com.backend.orbitflow.domain.todo.error.TodoErrorCode;
import com.backend.orbitflow.domain.todo.repository.TodoRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;
    private final CategoryService categoryService;
    private final TodoAuthorityService todoAuthorityService;
    private final RoutineService routineService;

    // 개인 투두의 담당자는 카테고리 소유자, 팀 투두는 지정한 구성원(미지정 시 생성자)
    public TodoResponse createTodo(
            User actor, Long categoryId, Long parentTodoId, User assignee, TodoType type, String name,
            LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes, RoutineRequest routine
    ) {
        Category category = categoryService.getActiveCategory(categoryId);
        todoAuthorityService.checkEdit(category, actor);
        validatePeriod(startDate, endDate);

        Todo parent = null;
        int sortOrder = 0;
        if (parentTodoId != null) {
            parent = getActiveTodo(parentTodoId);
            if (parent.isChild() || !parent.getCategory().getId().equals(category.getId())) {
                throw new CommonException(TodoErrorCode.INVALID_PARENT_TODO);
            }
            if (routine != null) {
                throw new CommonException(TodoErrorCode.CHILD_TODO_ROUTINE);
            }
            sortOrder = todoRepository.findMaxSortOrderByParent(parent) + 1;
        }

        User todoAssignee = category.getUser();
        if (category.isTeamCategory()) {
            todoAssignee = assignee == null ? actor : assignee;
            if (!todoAssignee.getId().equals(actor.getId())) {
                todoAuthorityService.checkAssign(category, actor, todoAssignee);
            }
        }

        Todo todo = todoRepository.save(Todo.of(
                category, parent, todoAssignee, type, name, startDate, endDate, remindBeforeMinutes, sortOrder
        ));
        if (routine != null) {
            routineService.upsertRoutine(todo, routine.durationType(), routine.duration(), routine.daysOfWeek(), routine.repeatEndDate());
        }
        // TODO: 알림 도메인 구현 후 remindBeforeMinutes 기준 리마인드(REMINDER) 예약
        return TodoResponse.from(todo);
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> getCategoryTodos(User viewer, Long categoryId) {
        Category category = categoryService.getActiveCategory(categoryId);
        todoAuthorityService.checkView(category, viewer);
        return toResponses(todoRepository.findActiveByCategory(category));
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodo(User viewer, Long todoId) {
        Todo todo = getActiveTodo(todoId);
        todoAuthorityService.checkView(todo.getCategory(), viewer);
        return TodoResponse.from(todo);
    }

    // 선택일과 기간이 겹치는 개인 투두 (팀 투두 제외)
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(User me, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        List<TodoResponse> schedules = todoRepository.findPersonalTodosBetween(me, TodoType.SCHEDULE, from, to).stream()
                .sorted(Comparator.comparing(Todo::getStartDate)
                        .thenComparingInt(Todo::getSortOrder)
                        .thenComparing(Todo::getId))
                .map(TodoResponse::from)
                .toList();

        Map<Category, List<TodoResponse>> backlogsByCategory = new LinkedHashMap<>();
        for (Todo todo : todoRepository.findPersonalTodosBetween(me, TodoType.BACKLOG, from, to)) {
            backlogsByCategory.computeIfAbsent(todo.getCategory(), key -> new ArrayList<>())
                    .add(TodoResponse.from(todo));
        }
        List<DashboardResponse.BacklogGroup> backlogs = backlogsByCategory.entrySet().stream()
                .map(entry -> DashboardResponse.BacklogGroup.of(entry.getKey(), entry.getValue()))
                .toList();

        return new DashboardResponse(date, schedules, backlogs, routineService.getPreviews(me, from, to));
    }

    public TodoResponse updateTodo(User actor, Long todoId, TodoType type, String name, LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes) {
        Todo todo = getActiveTodo(todoId);
        todoAuthorityService.checkEdit(todo.getCategory(), actor);
        validatePeriod(startDate, endDate);
        todo.updateTodo(type, name, startDate, endDate, remindBeforeMinutes);
        return TodoResponse.from(todo);
    }

    public TodoResponse toggleComplete(User actor, Long todoId) {
        Todo todo = getActiveTodo(todoId);
        todoAuthorityService.checkComplete(todo, actor);
        todo.toggleComplete();
        // TODO: 알림 도메인 구현 후 완료 시 팔로워에게 TODO_COMPLETED 알림, 타임라인 노출
        return TodoResponse.from(todo);
    }

    // 팀 투두의 담당자는 1명, 새 담당자 지정 시 기존 담당자는 즉시 해제
    public TodoResponse updateAssignee(User actor, Long todoId, User assignee) {
        Todo todo = getActiveTodo(todoId);
        if (!todo.getCategory().isTeamCategory()) {
            throw new CommonException(TodoErrorCode.NOT_TEAM_TODO);
        }
        todoAuthorityService.checkAssign(todo.getCategory(), actor, assignee);
        todo.updateAssignee(assignee);
        return TodoResponse.from(todo);
    }

    public List<TodoResponse> updateChildOrder(User actor, Long parentTodoId, List<Long> childTodoIds) {
        Todo parent = getActiveTodo(parentTodoId);
        todoAuthorityService.checkEdit(parent.getCategory(), actor);
        List<Todo> children = todoRepository.findAllByParentTodoAndDeletedAtIsNullOrderBySortOrderAsc(parent);

        Set<Long> requested = new HashSet<>(childTodoIds);
        Set<Long> current = new HashSet<>(children.stream().map(Todo::getId).toList());
        if (requested.size() != childTodoIds.size() || !requested.equals(current)) {
            throw new CommonException(TodoErrorCode.INVALID_CHILD_ORDER);
        }
        Map<Long, Todo> childById = new LinkedHashMap<>();
        children.forEach(child -> childById.put(child.getId(), child));
        for (int i = 0; i < childTodoIds.size(); i++) {
            childById.get(childTodoIds.get(i)).updateSortOrder(i);
        }
        return childTodoIds.stream()
                .map(id -> TodoResponse.from(childById.get(id)))
                .toList();
    }

    // 삭제 확정 즉시 목록에서 제외, 부모 투두 삭제 시 자식 투두도 함께 삭제
    public void deleteTodo(User actor, Long todoId) {
        Todo todo = getActiveTodo(todoId);
        todoAuthorityService.checkEdit(todo.getCategory(), actor);
        todo.delete();
        if (!todo.isChild()) {
            todoRepository.softDeleteChildren(todo, todo.getDeletedAt());
        }
        // TODO: 게시글 도메인 구현 후 연결된 게시글에 투두 삭제 표시
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> getRestorableTodos(User actor, Long categoryId) {
        Category category = categoryService.getActiveCategory(categoryId);
        todoAuthorityService.checkEdit(category, actor);
        return toResponses(todoRepository.findRestorableByCategory(category, retentionThreshold()));
    }

    // 삭제 후 30일 이내에만 원래 카테고리와 유형으로 복구, 함께 삭제된 자식 투두도 복구
    public TodoResponse restoreTodo(User actor, Long todoId) {
        Todo todo = todoRepository.findWithAllById(todoId).orElseThrow(
                () -> new CommonException(TodoErrorCode.TODO_NOT_FOUND)
        );
        if (!todo.isDeleted()) {
            throw new CommonException(TodoErrorCode.NOT_DELETED_TODO);
        }
        if (!todo.getDeletedAt().isAfter(retentionThreshold())) {
            throw new CommonException(TodoErrorCode.RESTORE_EXPIRED);
        }
        Category category = categoryService.getActiveCategory(todo.getCategory().getId());
        todoAuthorityService.checkEdit(category, actor);
        if (todo.isChild() && todo.getParentTodo().isDeleted()) {
            throw new CommonException(TodoErrorCode.PARENT_TODO_DELETED);
        }
        LocalDateTime deletedAt = todo.getDeletedAt();
        todo.restore();
        if (!todo.isChild()) {
            todoRepository.restoreChildren(todo, deletedAt);
        }
        return TodoResponse.from(todo);
    }

    public RoutineResponse upsertRoutine(User actor, Long todoId, RoutineRequest request) {
        Todo todo = getActiveTodo(todoId);
        todoAuthorityService.checkEdit(todo.getCategory(), actor);
        return routineService.upsertRoutine(todo, request.durationType(), request.duration(), request.daysOfWeek(), request.repeatEndDate());
    }

    public void deleteRoutine(User actor, Long todoId) {
        Todo todo = getActiveTodo(todoId);
        todoAuthorityService.checkEdit(todo.getCategory(), actor);
        routineService.deleteRoutine(todo);
    }

    // 소유자가 탈퇴했거나 팀이 삭제된 카테고리의 투두도 존재하지 않는 것으로 처리
    private Todo getActiveTodo(Long todoId) {
        Todo todo = todoRepository.findWithAllById(todoId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new CommonException(TodoErrorCode.TODO_NOT_FOUND));
        categoryService.getActiveCategory(todo.getCategory().getId());
        return todo;
    }

    private void validatePeriod(LocalDateTime startDate, LocalDateTime endDate) {
        if (endDate.isBefore(startDate)) {
            throw new CommonException(TodoErrorCode.INVALID_TODO_PERIOD);
        }
    }

    private LocalDateTime retentionThreshold() {
        return LocalDateTime.now().minusDays(RETENTION_DAYS);
    }

    private List<TodoResponse> toResponses(List<Todo> todos) {
        return todos.stream()
                .map(TodoResponse::from)
                .toList();
    }
}
