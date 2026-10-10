package com.backend.orbitflow.domain.todo.entity;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.todo.enums.TodoType;
import com.backend.orbitflow.domain.todo.listener.TodoReminderListener;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.SoftDeleteEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
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
        indexes = {
                // 타임라인 투두 완료 활동 조회
                @Index(columnList = "assignee_id, completed_at"),
                // 대시보드 기간 조회 : 지난 투두(end_date < from)는 계속 쌓이고 현재·이후 투두는 적으므로
                // end_date 범위로 사용자의 전체 이력 대신 현재·이후 행만 읽음 (category_id FK 인덱스 대체 가능)
                @Index(columnList = "category_id, type, end_date")
        })
// 저장·수정 시 리마인드 예약 목록(Redis) 갱신
@EntityListeners(TodoReminderListener.class)
public class Todo extends SoftDeleteEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Category category;

    // 한 단계의 자식 투두만 허용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_todo_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Todo parentTodo;

    // 반복으로 생성된 투두 (원본 투두 포함)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
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

    // 반복 회차의 원래 시각 (회차 생성 시 설정, 수정해도 유지) : 원본 투두·일반 투두는 null
    // 회차 슬롯 = coalesce(occurrenceDate, startDate) : 회차를 옮겨도 원래 슬롯이 다시 생성되지 않도록 구분
    @Column(name = "occurrence_date")
    private LocalDateTime occurrenceDate;

    public static Todo of(
            Category category, Todo parentTodo, User assignee, TodoType type, String name,
            LocalDateTime startDate, LocalDateTime endDate, Integer remindBeforeMinutes, int sortOrder
    ) {
        return new Todo(
                null, category, parentTodo, null, assignee, type, name,
                startDate, endDate, false, null, remindBeforeMinutes, sortOrder, null
        );
    }

    // 원본 투두를 복사해 startDate에 시작하는 반복 회차 생성 (기간 길이 유지)
    // origin이 지연 로딩 프록시일 수 있으므로 필드가 아니라 getter로 읽음 (프록시의 필드는 초기화되지 않아 null)
    public static Todo occurrence(Todo origin, Routine routine, LocalDateTime startDate) {
        Duration period = Duration.between(origin.getStartDate(), origin.getEndDate());
        return new Todo(
                null, origin.getCategory(), null, routine, origin.getAssignee(), origin.getType(), origin.getName(),
                startDate, startDate.plus(period), false, null, origin.getRemindBeforeMinutes(), 0, startDate
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

    // 회차를 새 반복의 원본 투두로 전환 (원본 투두는 원래 시각이 없음)
    public void promoteToOrigin(Routine routine) {
        this.routine = routine;
        this.occurrenceDate = null;
    }

    public boolean isChild() {
        return this.parentTodo != null;
    }

    public boolean isDeleted() {
        return getDeletedAt() != null;
    }

    // 리마인드 발송 시각 (컬럼 아님) : 리마인드 미설정·완료·삭제된 투두는 null
    public LocalDateTime getRemindAt() {
        if (remindBeforeMinutes == null || isCompleted || isDeleted()) {
            return null;
        }
        return startDate.minusMinutes(remindBeforeMinutes);
    }
}
