package com.backend.orbitflow.domain.avatar.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AvatarErrorCode implements ErrorCode {

    AVATAR_NOT_FOUND(HttpStatus.NOT_FOUND,
            "아바타가 존재하지 않습니다.",
            "https://orbitflow.com/errors/avatar-not-found",
            "Avatar Not Found"),
    ITEM_NOT_ON_SALE(HttpStatus.BAD_REQUEST,
            "판매 중인 아이템이 아닙니다.",
            "https://orbitflow.com/errors/item-not-on-sale",
            "Item Not On Sale"),
    ALREADY_OWNED_ITEM(HttpStatus.CONFLICT,
            "이미 보유한 아이템입니다.",
            "https://orbitflow.com/errors/already-owned-item",
            "Already Owned Item"),
    NOT_OWNED_ITEM(HttpStatus.NOT_FOUND,
            "보유하지 않은 아이템입니다.",
            "https://orbitflow.com/errors/not-owned-item",
            "Not Owned Item");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
