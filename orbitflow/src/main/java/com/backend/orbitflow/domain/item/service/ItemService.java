package com.backend.orbitflow.domain.item.service;

import com.backend.orbitflow.domain.item.dto.response.ItemResponse;
import com.backend.orbitflow.domain.item.enums.ItemType;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface ItemService {

    Page<ItemResponse> getShopItems(User me, ItemType type, int page, int size);

    // 관리자
    Page<ItemResponse> searchItems(ItemType type, Boolean onSale, int page, int size);
    ItemResponse createItem(String name, ItemType type, int price, MultipartFile image);
    ItemResponse updateItem(Long itemId, String name, ItemType type, int price, MultipartFile image);
    ItemResponse updateOnSale(Long itemId, boolean onSale);
}
