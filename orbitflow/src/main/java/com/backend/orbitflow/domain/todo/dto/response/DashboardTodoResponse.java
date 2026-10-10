package com.backend.orbitflow.domain.todo.dto.response;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.todo.entity.Routine;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.TodoType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Duration;
import java.time.LocalDateTime;

// 개인 대시보드 한 항목 (평면 구조, 대시보드 응답은 이 항목의 1차원 배열)
// 정렬 : SCHEDULE(시작 시각순) → BACKLOG(카테고리 생성순 → 시작 시각순), FE는 type으로 일정·백로그 칸반을 나눔
// preview = true : 아직 생성되지 않은 반복 회차 (id 없음, 작업 시 POST /api/routines/{routineId}/occurrences 로 생성)
public record DashboardTodoResponse(
        Long id,
        boolean preview,
        Long routineId,
        Long parentTodoId,
        TodoType type,
        String name,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startDate,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime endDate,
        boolean isCompleted,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime completedAt,
        Integer remindBeforeMinutes,
        int sortOrder,
        Long categoryId,
        String categoryName,
        String categoryColor
) {

    // 저장된 투두 (TodoRepository JPQL 생성자 표현식에서 사용)
    public DashboardTodoResponse(
            Long id, Long routineId, Long parentTodoId, TodoType type, String name,
            LocalDateTime startDate, LocalDateTime endDate, boolean isCompleted, LocalDateTime completedAt,
            Integer remindBeforeMinutes, int sortOrder, Long categoryId, String categoryName, String categoryColor
    ) {
        this(id, false, routineId, parentTodoId, type, name, startDate, endDate, isCompleted, completedAt,
                remindBeforeMinutes, sortOrder, categoryId, categoryName, categoryColor);
    }

    // 미리보기 회차 : 원본 투두의 내용·기간을 회차 시작 시각으로 옮김
    public static DashboardTodoResponse preview(Routine routine, LocalDateTime startDate) {
        Todo origin = routine.getTodo();
        Category category = origin.getCategory();
        return new DashboardTodoResponse(
                null, true, routine.getId(), null, origin.getType(), origin.getName(),
                startDate, startDate.plus(Duration.between(origin.getStartDate(), origin.getEndDate())),
                false, null, origin.getRemindBeforeMinutes(), origin.getSortOrder(),
                category.getId(), category.getName(), category.getColor()
        );
    }
}
