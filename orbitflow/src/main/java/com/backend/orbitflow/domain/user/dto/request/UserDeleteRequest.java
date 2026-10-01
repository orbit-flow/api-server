package com.backend.orbitflow.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserDeleteRequest(
        @NotBlank(message = "사용자 이름을 확인해 주세요.")
        String confirmName
) {
}
