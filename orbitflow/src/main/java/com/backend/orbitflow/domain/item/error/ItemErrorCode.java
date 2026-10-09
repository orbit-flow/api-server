package com.backend.orbitflow.domain.item.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ItemErrorCode implements ErrorCode {

    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 아이템입니다.",
            "https://orbitflow.com/errors/item-not-found",
            "Item Not Found"),
    ITEM_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST,
            "아이템 이미지는 필수입니다.",
            "https://orbitflow.com/errors/item-image-required",
            "Item Image Required");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
