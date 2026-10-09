package com.backend.orbitflow.domain.item.controller;

import com.backend.orbitflow.domain.item.dto.response.ItemResponse;
import com.backend.orbitflow.domain.item.dto.response.ItemSuccessCode;
import com.backend.orbitflow.domain.item.enums.ItemType;
import com.backend.orbitflow.domain.item.facade.ItemFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
public class ItemController {

    private final ItemFacade itemFacade;

    // 상점 : 판매 중인 아이템과 보유 여부 (type 미지정 시 전체 부위)
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<ItemResponse>>> getShopItems(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) ItemType type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ItemSuccessCode.GET_SHOP_ITEM_LIST,
                        itemFacade.getShopItems(authUser, type, page, size)
                ));
    }
}
