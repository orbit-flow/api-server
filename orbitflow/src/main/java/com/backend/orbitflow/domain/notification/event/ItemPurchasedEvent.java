package com.backend.orbitflow.domain.notification.event;

// 꾸밈 요소(아이템) 구매
public record ItemPurchasedEvent(Long userId, Long itemId, int price, int balanceAfter) {
}
