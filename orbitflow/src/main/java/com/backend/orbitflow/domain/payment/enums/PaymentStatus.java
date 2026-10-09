package com.backend.orbitflow.domain.payment.enums;

// READY(주문 생성) -> APPROVED(결제 승인·포인트 적립) -> REFUNDED(환불·포인트 차감), 승인 실패 시 FAILED
public enum PaymentStatus {
    READY, APPROVED, FAILED, REFUNDED
}
