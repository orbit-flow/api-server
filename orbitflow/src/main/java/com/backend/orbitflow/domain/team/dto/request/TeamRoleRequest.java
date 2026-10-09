package com.backend.orbitflow.domain.team.dto.request;

import com.backend.orbitflow.domain.team.enums.TeamPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TeamRoleRequest(
        @NotBlank(message = "역할 이름은 비어있을 수 없습니다.")
        @Size(max = 50, message = "역할 이름은 50자 이내여야 합니다.")
        String name,

        // null이면 'none'
        @Size(max = 20, message = "색상 코드는 20자 이내여야 합니다.")
        String color,

        int priority,

        @NotNull(message = "권한 목록은 비어있을 수 없습니다.")
        List<TeamPermission> permissions
) {
}
