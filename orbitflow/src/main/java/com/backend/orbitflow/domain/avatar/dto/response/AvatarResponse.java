package com.backend.orbitflow.domain.avatar.dto.response;

import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.avatar.entity.UserItem;
import com.backend.orbitflow.domain.item.entity.Item;
import com.backend.orbitflow.domain.item.enums.ItemType;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.Comparator;
import java.util.List;

// point : 본인 아바타 조회 시에만 포함 (다른 사용자 조회 시 null)
// equippedItems가 없는 부위는 FE에서 기본 아바타 이미지로 렌더링
public record AvatarResponse(
        String userUuid,
        String userName,
        LevelResponse level,
        Integer point,
        List<EquippedItem> equippedItems
) {

    public record EquippedItem(ItemType type, Long itemId, String name, String imageUrl) {
    }

    public static AvatarResponse of(Avatar avatar, User user, List<UserItem> equipped, boolean includePoint) {
        return new AvatarResponse(
                user.getUuid(),
                user.getName(),
                LevelResponse.from(avatar),
                includePoint ? avatar.getPoint() : null,
                equipped.stream()
                        .map(UserItem::getItem)
                        .sorted(Comparator.comparing(Item::getType))
                        .map(item -> new EquippedItem(item.getType(), item.getId(), item.getName(), item.getImageUrl()))
                        .toList()
        );
    }
}
