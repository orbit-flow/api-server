package com.backend.orbitflow.domain.avatar.dto.response;

// 구매 결과 : 차감된 포인트와 구매 후 잔액
public record PurchaseResponse(
        Long itemId,
        String name,
        int price,
        int balanceAfter
) {
}
