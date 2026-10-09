package com.backend.orbitflow.domain.category.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 카테고리입니다.",
            "https://orbitflow.com/errors/category-not-found",
            "Category Not Found"),
    CATEGORY_ACCESS_DENIED(HttpStatus.FORBIDDEN,
            "카테고리를 조회할 권한이 없습니다.",
            "https://orbitflow.com/errors/category-access-denied",
            "Category Access Denied"),
    CATEGORY_EDIT_DENIED(HttpStatus.FORBIDDEN,
            "카테고리를 수정할 권한이 없습니다.",
            "https://orbitflow.com/errors/category-edit-denied",
            "Category Edit Denied"),
    INVALID_MOVE_TARGET(HttpStatus.BAD_REQUEST,
            "투두를 옮길 카테고리는 같은 소유자의 다른 카테고리여야 합니다.",
            "https://orbitflow.com/errors/invalid-move-target",
            "Invalid Move Target"),
    NOT_TEAM_CATEGORY(HttpStatus.BAD_REQUEST,
            "팀 카테고리에만 열람 권한을 지정할 수 있습니다.",
            "https://orbitflow.com/errors/not-team-category",
            "Not Team Category");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
