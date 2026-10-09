package com.backend.orbitflow.domain.avatar.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AvatarSuccessCode implements SuccessCode {

    GET_MY_AVATAR(HttpStatus.OK, "내 아바타가 열람되었습니다."),
    GET_USER_AVATAR(HttpStatus.OK, "아바타가 열람되었습니다."),
    GET_MY_ITEMS(HttpStatus.OK, "보유 아이템 리스트가 열람되었습니다."),
    ITEM_PURCHASE(HttpStatus.OK, "아이템을 구매했습니다."),
    ITEM_EQUIP(HttpStatus.OK, "아이템을 장착했습니다."),
    ITEM_UNEQUIP(HttpStatus.OK, "아이템 장착을 해제했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
