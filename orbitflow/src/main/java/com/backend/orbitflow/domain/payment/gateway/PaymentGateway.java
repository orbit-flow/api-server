package com.backend.orbitflow.domain.payment.gateway;

// PG 연동 추상화 : 실제 PG(토스페이먼츠 등) 연동 시 구현체만 교체
// 구현체는 짧은 타임아웃을 둘 것 (승인·취소 호출 동안 결제 행 락을 보유하므로)
public interface PaymentGateway {

    // 결제 승인 확인, 실패 시 PaymentGatewayException
    void confirm(String paymentKey, String orderId, int amount);

    // 결제 취소(환불), 실패 시 PaymentGatewayException
    void cancel(String paymentKey, String reason);
}
