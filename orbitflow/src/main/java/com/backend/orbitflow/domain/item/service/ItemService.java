package com.backend.orbitflow.domain.item.service;

import com.backend.orbitflow.domain.item.dto.response.ItemResponse;
import com.backend.orbitflow.domain.item.enums.ItemType;
import com.backend.orbitflow.domain.item.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface ItemService {

    // 판매 중인 아이템 (보유 여부는 호출 측에서 아바타 도메인으로 조회)
    Page<Item> getShopItems(ItemType type, int page, int size);
    Item getItem(Long itemId);

    // 관리자
    Page<ItemResponse> searchItems(ItemType type, Boolean onSale, int page, int size);
    ItemResponse createItem(String name, ItemType type, int price, MultipartFile image);
    ItemResponse updateItem(Long itemId, String name, ItemType type, int price, MultipartFile image);
    ItemResponse updateOnSale(Long itemId, boolean onSale);
}
