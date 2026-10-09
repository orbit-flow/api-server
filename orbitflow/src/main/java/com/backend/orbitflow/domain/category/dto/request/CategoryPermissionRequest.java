package com.backend.orbitflow.domain.category.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// PRIVATE 팀 카테고리의 열람 허용 대상 전체 목록 (기존 목록을 이 목록으로 교체)
public record CategoryPermissionRequest(
        @NotNull(message = "역할 목록은 비어있을 수 없습니다.")
        List<Long> roleIds,

        @NotNull(message = "팀원 목록은 비어있을 수 없습니다.")
        List<String> memberUuids
) {
}
