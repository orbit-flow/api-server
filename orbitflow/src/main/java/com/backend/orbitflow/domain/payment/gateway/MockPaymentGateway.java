package com.backend.orbitflow.domain.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// 개발용 PG (dev 프로파일에서만 등록)
// dev 외 프로파일에서는 PaymentGateway 빈이 없어 앱이 기동되지 않음 (실제 PG 연동 없이 운영되는 것을 막는 fail-safe)
// paymentKey가 "fail"로 시작하면 승인 실패, "cancel-fail"로 시작하면 취소 실패를 흉내냄
@Slf4j
@Component
@Profile("dev")
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
