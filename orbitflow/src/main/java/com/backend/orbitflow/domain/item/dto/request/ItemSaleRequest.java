package com.backend.orbitflow.domain.item.dto.request;

import jakarta.validation.constraints.NotNull;

public record ItemSaleRequest(
        // false면 판매 중지 (이미 보유한 사용자는 계속 장착 가능)
        @NotNull(message = "판매 여부는 비어있을 수 없습니다.")
        Boolean onSale
) {
}
