package com.backend.orbitflow.domain.category.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategorySuccessCode implements SuccessCode {

    CATEGORY_CREATE(HttpStatus.CREATED, "카테고리가 생성되었습니다."),
    GET_CATEGORY_LIST(HttpStatus.OK, "카테고리 리스트가 열람되었습니다."),
    GET_CATEGORY_INFO(HttpStatus.OK, "카테고리 정보가 열람되었습니다."),
    CATEGORY_UPDATE(HttpStatus.OK, "카테고리가 업데이트 되었습니다."),
    CATEGORY_DELETE(HttpStatus.OK, "카테고리가 삭제되었습니다."),
    GET_CATEGORY_PERMISSION(HttpStatus.OK, "카테고리 열람 권한이 열람되었습니다."),
    CATEGORY_PERMISSION_UPDATE(HttpStatus.OK, "카테고리 열람 권한이 변경되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
