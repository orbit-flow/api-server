package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.todo.dto.request.RoutineRequest;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.TodoType;
import com.backend.orbitflow.domain.todo.error.TodoErrorCode;
import com.backend.orbitflow.domain.todo.repository.TodoRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.domain.todo.reminder.TodoReminderQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import com.backend.orbitflow.domain.todo.dto.response.DashboardTodoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

// 카테고리 상태·권한 확인과 반복 설정은 호출 측(Facade)에서 수행 (서비스끼리 직접 호출하지 않음)
@Service
@RequiredArgsConstructor
@Transactional
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;
    private final TodoReminderQueue todoReminderQueue;

    // 소유자가 탈퇴했거나 팀이 삭제된 카테고리의 투두도 존재하지 않는 것으로 처리 (카테고리 확인은 호출 측)
    @Transactional(readOnly = true)
    public Todo getActiveTodo(Long todoId) {
        return todoRepository.findWithAllById(todoId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new CommonException(TodoErrorCode.TODO_NOT_FOUND));
    }

    public void validatePeriod(LocalDateTime startDate, LocalDateTime endDate) {
        if (endDate.isBefore(startDate)) {
            throw new CommonException(TodoErrorCode.INVALID_TODO_PERIOD);
        }
    }

    public void validateParent(Todo parent, Category category, RoutineRequest routine) {
        if (parent.isChild() || !parent.getCategory().getId().equals(category.getId())) {
            throw new CommonException(TodoErrorCode.INVALID_PARENT_TODO);
        }
        if (routine != null) {
            throw new CommonException(TodoErrorCode.CHILD_TODO_ROUTINE);
        }
    }

    // 개인 투두의 담당자는 카테고리 소유자, 팀 투두는 지정한 구성원(미지정 시 생성자)
    public User resolveAssignee(Category category, User actor, User assignee) {
        if (!category.isTeamCategory()) {
            return category.getUser();
        }
        return assignee == null ? actor : assignee;
    }

    public Todo createTodo(
            Category category, Todo parent, User assignee, TodoType type, String name,
            LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes
    ) {
        int sortOrder = parent == null ? 0 : todoRepository.findMaxSortOrderByParent(parent) + 1;
        // 리마인드는 TodoReminderScheduler가 remindBeforeMinutes 기준으로 발송
        return todoRepository.save(Todo.of(
                category, parent, assignee, type, name, startDate, endDate, remindBeforeMinutes, sortOrder
        ));
    }

    @Transactional(readOnly = true)
    public Page<TodoResponse> getCategoryTodos(Category category, int page, int size) {
        Pageable pageable = toPageable(page, size);
        return todoRepository.findActiveByCategory(category, pageable).map(TodoResponse::from);
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodo(Todo todo) {
        return TodoResponse.from(todo);
    }

    // 개인 대시보드 (선택일 기준, 팀 투두·타임라인 제외) : 일정·백로그·미리보기 회차를 각각의 페이지 API로 제공
    // 일정·백로그는 응답 항목으로 바로 페이지 조회 (목록 1 + 개수 1)
    @Transactional(readOnly = true)
    public Page<DashboardTodoResponse> getDashboardSchedules(User me, LocalDate date, int page, int size) {
        Pageable pageable = toPageable(page, size);
        LocalDateTime from = date.atStartOfDay();
        return todoRepository.findDashboardSchedules(me, TodoType.SCHEDULE, from, from.plusDays(1), pageable);
    }

    @Transactional(readOnly = true)
    public Page<DashboardTodoResponse> getDashboardBacklogs(User me, LocalDate date, int page, int size) {
        Pageable pageable = toPageable(page, size);
        LocalDateTime from = date.atStartOfDay();
        return todoRepository.findDashboardBacklogs(me, TodoType.BACKLOG, from, from.plusDays(1), pageable);
    }

    public TodoResponse updateTodo(Todo todo, TodoType type, String name, LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes) {
        validatePeriod(startDate, endDate);
        // 반복 원본의 시작 시각은 회차 계산의 기준 : 바꾸면 이후 회차가 원래 시각과 새 시각에 중복 생성됨
        if (todo.isRoutineOrigin() && !todo.getStartDate().equals(startDate)) {
            throw new CommonException(TodoErrorCode.ROUTINE_ORIGIN_START_FIXED);
        }
        todo.updateTodo(type, name, startDate, endDate, remindBeforeMinutes);
        return TodoResponse.from(todo);
    }

    public TodoResponse toggleComplete(Todo todo) {
        todo.toggleComplete();
        // 완료 활동은 팔로워 타임라인에 조회 시점에 노출 (TimelineService), 완료 취소 시 제외
        return TodoResponse.from(todo);
    }

    public void requireTeamTodo(Todo todo) {
        if (!todo.getCategory().isTeamCategory()) {
            throw new CommonException(TodoErrorCode.NOT_TEAM_TODO);
        }
    }

    // 팀 투두의 담당자는 1명, 새 담당자 지정 시 기존 담당자는 즉시 해제
    public TodoResponse updateAssignee(Todo todo, User assignee) {
        todo.updateAssignee(assignee);
        return TodoResponse.from(todo);
    }

    public void updateChildOrder(Todo parent, List<Long> childTodoIds) {
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
    }

    // 삭제 확정 즉시 목록에서 제외, 부모 투두 삭제 시 자식 투두도 함께 삭제
    public void deleteTodo(Todo todo) {
        todo.delete();
        if (!todo.isChild()) {
            todoRepository.softDeleteChildren(todo, todo.getDeletedAt());
        }
        // 연결된 게시글은 유지되며 PostResponse.todo.deleted로 삭제 여부 표시
    }

    @Transactional(readOnly = true)
    public Page<TodoResponse> getRestorableTodos(Category category, int page, int size) {
        Pageable pageable = toPageable(page, size);
        return todoRepository.findRestorableByCategory(category, retentionThreshold(), pageable).map(TodoResponse::from);
    }

    // 삭제 후 30일 이내에만 원래 카테고리와 유형으로 복구 (카테고리 상태·권한 확인은 호출 측)
    @Transactional(readOnly = true)
    public Todo getRestorableTodo(Long todoId) {
        Todo todo = todoRepository.findWithAllById(todoId).orElseThrow(
                () -> new CommonException(TodoErrorCode.TODO_NOT_FOUND)
        );
        if (!todo.isDeleted()) {
            throw new CommonException(TodoErrorCode.NOT_DELETED_TODO);
        }
        if (!todo.getDeletedAt().isAfter(retentionThreshold())) {
            throw new CommonException(TodoErrorCode.RESTORE_EXPIRED);
        }
        return todo;
    }

    // 함께 삭제된 자식 투두도 복구
    public TodoResponse restoreTodo(Todo todo) {
        if (todo.isChild() && todo.getParentTodo().isDeleted()) {
            throw new CommonException(TodoErrorCode.PARENT_TODO_DELETED);
        }
        LocalDateTime deletedAt = todo.getDeletedAt();
        todo.restore();
        if (!todo.isChild()) {
            // 자식 투두 복구는 JPQL 일괄 변경이라 엔티티 리스너가 동작하지 않으므로 복구 전에 리마인드 대상을 조회해 직접 예약
            List<TodoRepository.ReminderTarget> reminders = todoRepository.findReminderTargetsByParentAndDeletedAt(todo, deletedAt);
            todoRepository.restoreChildren(todo, deletedAt);
            reminders.forEach(target -> todoReminderQueue.scheduleAfterCommit(
                    target.getId(), target.getStartDate().minusMinutes(target.getRemindBeforeMinutes())));
        }
        return TodoResponse.from(todo);
    }

    @Transactional(readOnly = true)
    public Optional<Todo> findTodo(Long todoId) {
        return todoRepository.findWithAllById(todoId);
    }

    private LocalDateTime retentionThreshold() {
        return LocalDateTime.now().minusDays(RETENTION_DAYS);
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
