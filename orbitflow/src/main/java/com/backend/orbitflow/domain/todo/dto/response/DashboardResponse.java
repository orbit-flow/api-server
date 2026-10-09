package com.backend.orbitflow.domain.todo.dto.response;

import com.backend.orbitflow.domain.category.entity.Category;

import java.time.LocalDate;
import java.util.List;

// 선택일 기준 개인 투두 (팀 투두·타임라인 제외)
// schedules : 일정 칸반 (시작 시각순), backlogs : 백로그 칸반 (카테고리별), previews : 아직 생성되지 않은 반복 회차
public record DashboardResponse(
        LocalDate date,
        List<TodoResponse> schedules,
        List<BacklogGroup> backlogs,
        List<TodoPreviewResponse> previews
) {

    public record BacklogGroup(
            Long categoryId,
            String categoryName,
            String categoryColor,
            List<TodoResponse> todos
    ) {

        public static BacklogGroup of(Category category, List<TodoResponse> todos) {
            return new BacklogGroup(
                    category.getId(),
                    category.getName(),
                    category.getColor(),
                    todos
            );
        }
    }
}
