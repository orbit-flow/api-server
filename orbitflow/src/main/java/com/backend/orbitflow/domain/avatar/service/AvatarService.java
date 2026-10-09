package com.backend.orbitflow.domain.avatar.service;

import com.backend.orbitflow.domain.avatar.dto.response.AvatarResponse;
import com.backend.orbitflow.domain.avatar.dto.response.PurchaseResponse;
import com.backend.orbitflow.domain.avatar.dto.response.UserItemResponse;
import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.item.entity.Item;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AvatarService {

    Avatar createAvatar(User user);
    Avatar getForUpdate(User user);
    AvatarResponse getMyAvatar(User me);
    AvatarResponse getUserAvatar(User target);
    List<UserItemResponse> getMyItems(User me);
    Set<Long> findOwnedItemIds(User me, Collection<Item> items);
    PurchaseResponse purchase(User me, Long itemId);
    UserItemResponse equip(User me, Long itemId);
    UserItemResponse unequip(User me, Long itemId);
}
