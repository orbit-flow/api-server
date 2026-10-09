package com.backend.orbitflow.domain.todo.entity;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.todo.enums.TodoType;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.SoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

// 논리적 삭제 후 30일 이내 복구 가능
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "todos",
        // 반복 회차 중복 생성 방지 (논리적 삭제된 회차 포함)
        uniqueConstraints = @UniqueConstraint(columnNames = {"routine_id", "start_date"}),
        // 타임라인 투두 완료 활동 조회
        indexes = @Index(columnList = "assignee_id, completed_at"))
public class Todo extends SoftDeleteEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 한 단계의 자식 투두만 허용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_todo_id")
    private Todo parentTodo;

    // 반복으로 생성된 투두 (원본 투두 포함)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private Routine routine;

    // 담당자는 비워둘 수 없음 (개인 투두는 카테고리 소유자)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignee_id", nullable = false)
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TodoType type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private boolean isCompleted;

    private LocalDateTime completedAt;

    // null이면 리마인드 없음 (투두당 1개)
    private Integer remindBeforeMinutes;

    @Column(nullable = false)
    private int sortOrder;

    public static Todo of(
            Category category, Todo parentTodo, User assignee, TodoType type, String name,
            LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes, int sortOrder
    ) {
        return new Todo(
                null, category, parentTodo, null, assignee, type, name,
                startDate, endDate, false, null, remindBeforeMinutes, sortOrder
        );
    }

    // 원본 투두를 복사해 startDate에 시작하는 반복 회차 생성 (기간 길이 유지)
    public static Todo occurrence(Todo origin, Routine routine, LocalDateTime startDate) {
        Duration period = Duration.between(origin.startDate, origin.endDate);
        return new Todo(
                null, origin.category, null, routine, origin.assignee, origin.type, origin.name,
                startDate, startDate.plus(period), false, null, origin.remindBeforeMinutes, 0
        );
    }

    public void updateTodo(TodoType type, String name, LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes) {
        this.type = type;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.remindBeforeMinutes = remindBeforeMinutes;
    }

    public void toggleComplete() {
        this.isCompleted = !this.isCompleted;
        this.completedAt = this.isCompleted ? LocalDateTime.now() : null;
    }

    public void updateAssignee(User assignee) {
        this.assignee = assignee;
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void updateRoutine(Routine routine) {
        this.routine = routine;
    }

    public boolean isChild() {
        return this.parentTodo != null;
    }

    public boolean isDeleted() {
        return getDeletedAt() != null;
    }
}
