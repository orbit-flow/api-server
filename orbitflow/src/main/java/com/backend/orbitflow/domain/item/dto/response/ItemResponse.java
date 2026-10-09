package com.backend.orbitflow.domain.item.dto.response;

import com.backend.orbitflow.domain.item.entity.Item;
import com.backend.orbitflow.domain.item.enums.ItemType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// owned : 상점 조회 시 요청자의 보유 여부 (관리자 조회 시 null)
public record ItemResponse(
        Long id,
        String name,
        ItemType type,
        int price,
        String imageUrl,
        boolean isOnSale,
        Boolean owned,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public static ItemResponse of(Item item, Boolean owned) {
        return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getType(),
                item.getPrice(),
                item.getImageUrl(),
                item.isOnSale(),
                owned,
                item.getCreatedAt()
        );
    }

    public static ItemResponse from(Item item) {
        return of(item, null);
    }
}
