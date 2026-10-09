package com.backend.orbitflow.domain.team.dto.request;

import jakarta.validation.constraints.NotBlank;

// 초대 대상, 소유자 위임 대상 등 사용자 지정용
public record TeamUserRequest(
        @NotBlank(message = "사용자 uuid는 비어있을 수 없습니다.")
        String userUuid
) {
}
