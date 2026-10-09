package com.backend.orbitflow.domain.item.facade;

import com.backend.orbitflow.domain.item.dto.request.ItemRequest;
import com.backend.orbitflow.domain.item.dto.request.ItemSaleRequest;
import com.backend.orbitflow.domain.item.dto.response.ItemResponse;
import com.backend.orbitflow.domain.item.enums.ItemType;
import com.backend.orbitflow.domain.item.service.ItemService;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ItemFacade {

    private final ItemService itemService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> getShopItems(AuthUser authUser, ItemType type, int page, int size) {
        return PageResponse.from(itemService.getShopItems(userService.getByUuid(authUser.getUuid()), type, page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> searchItems(ItemType type, Boolean onSale, int page, int size) {
        return PageResponse.from(itemService.searchItems(type, onSale, page, size));
    }

    @Transactional
    public ItemResponse createItem(ItemRequest request, MultipartFile image) {
        return itemService.createItem(request.name(), request.type(), request.price(), image);
    }

    @Transactional
    public ItemResponse updateItem(Long itemId, ItemRequest request, MultipartFile image) {
        return itemService.updateItem(itemId, request.name(), request.type(), request.price(), image);
    }

    @Transactional
    public ItemResponse updateOnSale(Long itemId, ItemSaleRequest request) {
        return itemService.updateOnSale(itemId, request.onSale());
    }
}
