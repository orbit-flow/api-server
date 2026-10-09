package com.backend.orbitflow.domain.payment.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 결제입니다.",
            "https://orbitflow.com/errors/payment-not-found",
            "Payment Not Found"),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT,
            "이미 처리된 결제입니다.",
            "https://orbitflow.com/errors/payment-already-processed",
            "Payment Already Processed"),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST,
            "결제 금액이 주문 금액과 일치하지 않습니다.",
            "https://orbitflow.com/errors/payment-amount-mismatch",
            "Payment Amount Mismatch"),
    PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_GATEWAY,
            "결제 승인에 실패했습니다.",
            "https://orbitflow.com/errors/payment-confirm-failed",
            "Payment Confirm Failed"),
    PAYMENT_NOT_REFUNDABLE(HttpStatus.BAD_REQUEST,
            "승인된 결제만 환불할 수 있습니다.",
            "https://orbitflow.com/errors/payment-not-refundable",
            "Payment Not Refundable"),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_GATEWAY,
            "결제 취소에 실패했습니다. 잠시 후 다시 시도해주세요.",
            "https://orbitflow.com/errors/payment-cancel-failed",
            "Payment Cancel Failed");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
