package com.backend.orbitflow.domain.payment.dto.response;

import com.backend.orbitflow.domain.payment.entity.Payment;
import com.backend.orbitflow.domain.payment.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// balanceAfter : 승인·환불 처리 직후의 포인트 잔액 (그 외 조회 시 null)
public record PaymentResponse(
        String orderId,
        int amount,
        int point,
        PaymentStatus status,
        Integer balanceAfter,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public static PaymentResponse of(Payment payment, Integer balanceAfter) {
        return new PaymentResponse(
                payment.getOrderId(),
                payment.getAmount(),
                payment.getPoint(),
                payment.getStatus(),
                balanceAfter,
                payment.getCreatedAt()
        );
    }

    public static PaymentResponse from(Payment payment) {
        return of(payment, null);
    }
}
