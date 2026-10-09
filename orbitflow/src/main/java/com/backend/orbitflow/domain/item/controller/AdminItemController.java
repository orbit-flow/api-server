package com.backend.orbitflow.domain.item.controller;

import com.backend.orbitflow.domain.item.dto.request.ItemRequest;
import com.backend.orbitflow.domain.item.dto.request.ItemSaleRequest;
import com.backend.orbitflow.domain.item.dto.response.ItemResponse;
import com.backend.orbitflow.domain.item.dto.response.ItemSuccessCode;
import com.backend.orbitflow.domain.item.enums.ItemType;
import com.backend.orbitflow.domain.item.facade.ItemFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// 플랫폼 관리자 전용 (SecurityConfig에서 /api/admin/** ROLE_ADMIN 제한)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/items")
public class AdminItemController {

    private final ItemFacade itemFacade;

    // 판매 중지 아이템 포함 (type·onSale 미지정 시 전체)
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<ItemResponse>>> searchItems(
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) Boolean onSale,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ItemSuccessCode.GET_ITEM_LIST,
                        itemFacade.searchItems(type, onSale, page, size)
                ));
    }

    // multipart : request(JSON) + image(필수)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ItemResponse>> createItem(
            @Valid @RequestPart("request") ItemRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        ItemSuccessCode.ITEM_CREATE,
                        itemFacade.createItem(request, image)
                ));
    }

    // multipart : request(JSON) + image(선택, 없으면 기존 이미지 유지)
    @PutMapping(value = "/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ItemResponse>> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestPart("request") ItemRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ItemSuccessCode.ITEM_UPDATE,
                        itemFacade.updateItem(itemId, request, image)
                ));
    }

    // 판매 시작·중지
    @PatchMapping("/{itemId}/sale")
    public ResponseEntity<CommonResponse<ItemResponse>> updateOnSale(
            @PathVariable Long itemId,
            @Valid @RequestBody ItemSaleRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ItemSuccessCode.ITEM_SALE_UPDATE,
                        itemFacade.updateOnSale(itemId, request)
                ));
    }
}
