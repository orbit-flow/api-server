package com.backend.orbitflow.domain.avatar.service;

import com.backend.orbitflow.domain.avatar.dto.response.AvatarResponse;
import com.backend.orbitflow.domain.avatar.dto.response.PurchaseResponse;
import com.backend.orbitflow.domain.avatar.dto.response.UserItemResponse;
import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.item.entity.Item;
import com.backend.orbitflow.domain.item.enums.ItemType;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.Collection;
import java.util.Set;
import org.springframework.data.domain.Page;

public interface AvatarService {

    Avatar createAvatar(User user);
    AvatarResponse getMyAvatar(User me);
    AvatarResponse getUserAvatar(User target);
    Page<UserItemResponse> getMyItems(User me, int page, int size);
    Set<Long> findOwnedItemIds(User me, Collection<Item> items);
    Avatar lockAvatar(User me);
    PurchaseResponse purchase(User me, Avatar avatar, Item item);
    UserItemResponse equip(User me, Long itemId);
    UserItemResponse unequip(User me, Long itemId);
    void unequipAllOnTypeChange(Item item, ItemType newType);
}
