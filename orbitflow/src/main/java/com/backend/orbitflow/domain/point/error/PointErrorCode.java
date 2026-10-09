package com.backend.orbitflow.domain.point.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PointErrorCode implements ErrorCode {

    ALREADY_ATTENDED(HttpStatus.CONFLICT,
            "오늘은 이미 출석했습니다.",
            "https://orbitflow.com/errors/already-attended",
            "Already Attended"),
    NOT_ENOUGH_POINT(HttpStatus.BAD_REQUEST,
            "포인트가 부족합니다.",
            "https://orbitflow.com/errors/not-enough-point",
            "Not Enough Point"),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 포인트 거래입니다.",
            "https://orbitflow.com/errors/transaction-not-found",
            "Transaction Not Found"),
    NOT_REVOCABLE_TRANSACTION(HttpStatus.BAD_REQUEST,
            "출석 적립 거래만 회수할 수 있습니다.",
            "https://orbitflow.com/errors/not-revocable-transaction",
            "Not Revocable Transaction"),
    ALREADY_REVOKED(HttpStatus.CONFLICT,
            "이미 회수된 거래입니다.",
            "https://orbitflow.com/errors/already-revoked",
            "Already Revoked");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
