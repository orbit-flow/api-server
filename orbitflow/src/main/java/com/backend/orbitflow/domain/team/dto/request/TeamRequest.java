package com.backend.orbitflow.domain.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamRequest(
        @NotBlank(message = "팀 이름은 비어있을 수 없습니다.")
        @Size(max = 50, message = "팀 이름은 50자 이내여야 합니다.")
        String name,

        // null이면 identicon 렌더링
        String icon
) {
}
