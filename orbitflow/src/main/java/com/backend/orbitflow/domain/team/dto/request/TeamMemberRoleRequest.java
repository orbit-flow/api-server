package com.backend.orbitflow.domain.team.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// 구성원이 가질 역할 전체 목록 (기존 역할을 이 목록으로 교체)
public record TeamMemberRoleRequest(
        @NotNull(message = "역할 목록은 비어있을 수 없습니다.")
        List<Long> roleIds
) {
}
