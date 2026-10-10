package com.backend.orbitflow.domain.item.facade;

import com.backend.orbitflow.domain.avatar.service.AvatarService;
import com.backend.orbitflow.domain.item.dto.request.ItemRequest;
import com.backend.orbitflow.domain.item.dto.request.ItemSaleRequest;
import com.backend.orbitflow.domain.item.dto.response.ItemResponse;
import com.backend.orbitflow.domain.item.entity.Item;
import com.backend.orbitflow.domain.item.enums.ItemType;
import com.backend.orbitflow.domain.item.service.ItemService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ItemFacade {

    private final ItemService itemService;
    private final UserService userService;
    private final AvatarService avatarService;

    // 판매 중인 아이템과 요청자의 보유 여부 (보유 여부는 페이지의 아이템만 한 번에 조회)
    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> getShopItems(AuthUser authUser, ItemType type, int page, int size) {
        User me = userService.getByUuid(authUser.getUuid());
        Page<Item> items = itemService.getShopItems(type, page, size);
        Set<Long> ownedIds = avatarService.findOwnedItemIds(me, items.getContent());
        return PageResponse.from(items.map(item -> ItemResponse.of(item, ownedIds.contains(item.getId()))));
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> searchItems(ItemType type, Boolean onSale, int page, int size) {
        return PageResponse.from(itemService.searchItems(type, onSale, page, size));
    }

    @Transactional
    public ItemResponse createItem(ItemRequest request, MultipartFile image) {
        return itemService.createItem(request.name(), request.type(), request.price(), image);
    }

    // 다른 부위로 바뀌면 같은 부위 중복 장착이 생길 수 있으므로 기존 장착을 먼저 해제
    @Transactional
    public ItemResponse updateItem(Long itemId, ItemRequest request, MultipartFile image) {
        Item item = itemService.getItem(itemId);
        if (item.getType() != request.type()) {
            avatarService.unequipAllByItem(item);
        }
        return itemService.updateItem(itemId, request.name(), request.type(), request.price(), image);
    }

    @Transactional
    public ItemResponse updateOnSale(Long itemId, ItemSaleRequest request) {
        return itemService.updateOnSale(itemId, request.onSale());
    }
}
