package com.backend.orbitflow.domain.avatar.controller;

import com.backend.orbitflow.domain.avatar.dto.response.AvatarResponse;
import com.backend.orbitflow.domain.avatar.dto.response.AvatarSuccessCode;
import com.backend.orbitflow.domain.avatar.dto.response.PurchaseResponse;
import com.backend.orbitflow.domain.avatar.dto.response.UserItemResponse;
import com.backend.orbitflow.domain.avatar.facade.AvatarFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AvatarController {

    private final AvatarFacade avatarFacade;

    // 레벨·경험치·포인트와 장착 아이템
    @GetMapping("/avatars/me")
    public ResponseEntity<CommonResponse<AvatarResponse>> getMyAvatar(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AvatarSuccessCode.GET_MY_AVATAR,
                        avatarFacade.getMyAvatar(authUser)
                ));
    }

    // 다른 사용자의 아바타 (포인트 비공개)
    @GetMapping("/users/{userUuid}/avatar")
    public ResponseEntity<CommonResponse<AvatarResponse>> getUserAvatar(
            @PathVariable String userUuid
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AvatarSuccessCode.GET_USER_AVATAR,
                        avatarFacade.getUserAvatar(userUuid)
                ));
    }

    @GetMapping("/avatars/me/items")
    public ResponseEntity<CommonResponse<List<UserItemResponse>>> getMyItems(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AvatarSuccessCode.GET_MY_ITEMS,
                        avatarFacade.getMyItems(authUser)
                ));
    }

    // 포인트로 상점 아이템 구매 (취소·환급 불가)
    @PostMapping("/items/{itemId}/purchase")
    public ResponseEntity<CommonResponse<PurchaseResponse>> purchase(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long itemId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AvatarSuccessCode.ITEM_PURCHASE,
                        avatarFacade.purchase(authUser, itemId)
                ));
    }

    // 같은 부위의 기존 장착 아이템은 자동 해제
    @PatchMapping("/avatars/me/items/{itemId}/equip")
    public ResponseEntity<CommonResponse<UserItemResponse>> equip(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long itemId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AvatarSuccessCode.ITEM_EQUIP,
                        avatarFacade.equip(authUser, itemId)
                ));
    }

    @PatchMapping("/avatars/me/items/{itemId}/unequip")
    public ResponseEntity<CommonResponse<UserItemResponse>> unequip(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long itemId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        AvatarSuccessCode.ITEM_UNEQUIP,
                        avatarFacade.unequip(authUser, itemId)
                ));
    }
}
