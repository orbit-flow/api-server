package com.backend.orbitflow.domain.payment.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentSuccessCode implements SuccessCode {

    GET_POINT_PACKAGES(HttpStatus.OK, "포인트 충전 상품이 열람되었습니다."),
    PAYMENT_CREATE(HttpStatus.CREATED, "결제 주문이 생성되었습니다."),
    PAYMENT_CONFIRM(HttpStatus.OK, "결제가 승인되어 포인트가 충전되었습니다."),
    PAYMENT_REFUND(HttpStatus.OK, "결제가 환불되었습니다."),
    GET_PAYMENT_LIST(HttpStatus.OK, "결제 내역이 열람되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
