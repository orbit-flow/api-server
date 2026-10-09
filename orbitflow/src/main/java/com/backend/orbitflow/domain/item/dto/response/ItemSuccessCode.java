package com.backend.orbitflow.domain.item.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ItemSuccessCode implements SuccessCode {

    GET_SHOP_ITEM_LIST(HttpStatus.OK, "상점 아이템 리스트가 열람되었습니다."),
    GET_ITEM_LIST(HttpStatus.OK, "아이템 리스트가 열람되었습니다."),
    ITEM_CREATE(HttpStatus.CREATED, "아이템이 추가되었습니다."),
    ITEM_UPDATE(HttpStatus.OK, "아이템이 수정되었습니다."),
    ITEM_SALE_UPDATE(HttpStatus.OK, "아이템 판매 상태가 변경되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
