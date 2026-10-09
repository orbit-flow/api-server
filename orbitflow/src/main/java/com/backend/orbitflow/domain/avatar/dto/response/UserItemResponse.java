package com.backend.orbitflow.domain.avatar.dto.response;

import com.backend.orbitflow.domain.avatar.entity.UserItem;
import com.backend.orbitflow.domain.item.entity.Item;
import com.backend.orbitflow.domain.item.enums.ItemType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// 판매 중지된 아이템도 보유·장착 가능
public record UserItemResponse(
        Long itemId,
        String name,
        ItemType type,
        String imageUrl,
        boolean isEquipped,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime purchasedAt
) {

    public static UserItemResponse from(UserItem userItem) {
        Item item = userItem.getItem();
        return new UserItemResponse(
                item.getId(),
                item.getName(),
                item.getType(),
                item.getImageUrl(),
                userItem.isEquipped(),
                userItem.getCreatedAt()
        );
    }
}
