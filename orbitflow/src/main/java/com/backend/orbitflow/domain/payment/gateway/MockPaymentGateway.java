package com.backend.orbitflow.domain.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 개발·테스트용 PG (payment.gateway=mock 또는 미설정 시 사용)
// paymentKey가 "fail"로 시작하면 승인 실패, "cancel-fail"로 시작하면 취소 실패를 흉내냄
@Slf4j
@Component
@ConditionalOnProperty(prefix = "payment", name = "gateway", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public void confirm(String paymentKey, String orderId, int amount) {
        if (paymentKey.startsWith("fail")) {
            throw new PaymentGatewayException("[Mock] 결제 승인 실패 : " + orderId);
        }
        log.info("[Mock PG] 결제 승인 : orderId={}, amount={}", orderId, amount);
    }

    @Override
    public void cancel(String paymentKey, String reason) {
        if (paymentKey.startsWith("cancel-fail")) {
            throw new PaymentGatewayException("[Mock] 결제 취소 실패 : " + paymentKey);
        }
        log.info("[Mock PG] 결제 취소 : paymentKey={}, reason={}", paymentKey, reason);
    }
}
