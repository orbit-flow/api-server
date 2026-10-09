package com.backend.orbitflow.domain.category.dto.request;

import com.backend.orbitflow.domain.category.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "카테고리 이름은 비어있을 수 없습니다.")
        @Size(max = 50, message = "카테고리 이름은 50자 이내여야 합니다.")
        String name,

        @Size(max = 20, message = "색상 코드는 20자 이내여야 합니다.")
        String color,

        // null이면 PUBLIC
        Visibility visibility
) {
}
