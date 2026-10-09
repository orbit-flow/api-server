package com.backend.orbitflow.domain.item.dto.request;

import com.backend.orbitflow.domain.item.enums.ItemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// multipart의 "request" 파트 (이미지는 "image" 파트)
public record ItemRequest(
        @NotBlank(message = "아이템 이름은 비어있을 수 없습니다.")
        @Size(max = 50, message = "아이템 이름은 50자 이내여야 합니다.")
        String name,

        @NotNull(message = "장착 부위는 비어있을 수 없습니다.")
        ItemType type,

        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        int price
) {
}
