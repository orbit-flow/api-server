package com.backend.orbitflow.domain.category.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import jakarta.validation.constraints.Size;

// PRIVATE 팀 카테고리의 열람 허용 대상 전체 목록 (기존 목록을 이 목록으로 교체)
public record CategoryPermissionRequest(
        @NotNull(message = "역할 목록은 비어있을 수 없습니다.")
        @Size(max = 100, message = "역할은 최대 100개까지 지정할 수 있습니다.")
        List<Long> roleIds,

        @NotNull(message = "팀원 목록은 비어있을 수 없습니다.")
        @Size(max = 200, message = "팀원은 최대 200명까지 지정할 수 있습니다.")
        List<String> memberUuids
) {
}
