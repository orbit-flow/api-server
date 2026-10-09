package com.backend.orbitflow.domain.todo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TodoAssigneeRequest(
        @NotBlank(message = "담당자 uuid는 비어있을 수 없습니다.")
        String userUuid
) {
}
