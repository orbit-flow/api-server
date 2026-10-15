package com.backend.orbitflow.domain.payment.service;

import com.backend.orbitflow.domain.payment.entity.PaymentRecovery;
import com.backend.orbitflow.domain.payment.gateway.PaymentGateway;
import com.backend.orbitflow.domain.payment.repository.PaymentRecoveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRecoveryService {

    private final PaymentRecoveryRepository recoveryRepository;
    private final PaymentGateway paymentGateway;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String orderId, String paymentKey) {
        recoveryRepository.findByOrderIdAndResolvedFalse(orderId)
                .orElseGet(() -> recoveryRepository.save(PaymentRecovery.of(orderId, paymentKey)));
    }

    @Transactional
    public void retry(PaymentRecovery recovery) {
        try {
            paymentGateway.cancel(recovery.getPaymentKey(), "승인 후 DB 반영 실패 자동 보상 취소");
            recovery.resolve();
        } catch (RuntimeException e) {
            recovery.retryLater();
            log.error("결제 보상 취소 재시도 실패 : orderId={}", recovery.getOrderId(), e);
        }
    }
}
