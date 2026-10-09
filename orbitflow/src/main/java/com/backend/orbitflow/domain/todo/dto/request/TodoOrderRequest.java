package com.backend.orbitflow.domain.todo.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// 자식 투두 id를 표시할 순서대로 전체 나열
public record TodoOrderRequest(
        @NotNull(message = "투두 순서는 비어있을 수 없습니다.")
        List<Long> todoIds
) {
}
