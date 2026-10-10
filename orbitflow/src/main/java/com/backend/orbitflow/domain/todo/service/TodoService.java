package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.todo.dto.request.RoutineRequest;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.entity.Todo;
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

    // 투두 조회·변경 전에 호출 측(Facade)이 CategoryService.getActiveCategory와 TodoAuthorityService로 카테고리 상태·권한을 확인
    // 삭제되지 않은 투두 (소속 카테고리가 유효한지는 호출 측에서 확인)
    Todo getActiveTodo(Long todoId);
    void validatePeriod(LocalDateTime startDate, LocalDateTime endDate);
    // 부모 투두로 지정할 수 있는지 (parent는 getActiveTodo로 조회한 투두)
    void validateParent(Todo parent, Category category, RoutineRequest routine);
    User resolveAssignee(Category category, User actor, User assignee);
    Todo createTodo(
            Category category, Todo parent, User assignee, TodoType type, String name,
            LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes
    );
    Page<TodoResponse> getCategoryTodos(Category category, int page, int size);
    TodoResponse getTodo(Todo todo);
    Page<DashboardTodoResponse> getDashboardSchedules(User me, LocalDate date, int page, int size);
    Page<DashboardTodoResponse> getDashboardBacklogs(User me, LocalDate date, int page, int size);
    TodoResponse updateTodo(Todo todo, TodoType type, String name, LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes);
    TodoResponse toggleComplete(Todo todo);
    void requireTeamTodo(Todo todo);
    TodoResponse updateAssignee(Todo todo, User assignee);
    void updateChildOrder(Todo parent, List<Long> childTodoIds);
    void deleteTodo(Todo todo);
    Page<TodoResponse> getRestorableTodos(Category category, int page, int size);
    // 복구 가능한 삭제 투두 (복구 가능 기간 이내)
    Todo getRestorableTodo(Long todoId);
    TodoResponse restoreTodo(Todo todo);

    // 다른 도메인에서 사용 : 논리적 삭제된 투두 포함 (카테고리·담당자와 함께 조회)
    Optional<Todo> findTodo(Long todoId);
}
