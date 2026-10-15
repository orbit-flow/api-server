package com.backend.orbitflow.domain.payment.scheduler;

import com.backend.orbitflow.domain.payment.repository.PaymentRecoveryRepository;
import com.backend.orbitflow.domain.payment.service.PaymentRecoveryService;
import com.backend.orbitflow.global.util.RedisDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {

    private final PaymentRecoveryRepository recoveryRepository;
    private final PaymentRecoveryService recoveryService;
    private final RedisDistributedLock distributedLock;

    @Scheduled(fixedDelay = 60_000L)
    public void retryCompensations() {
        distributedLock.run("payment-recovery", Duration.ofSeconds(55), () ->
                recoveryRepository.findTop100ByResolvedFalseAndNextAttemptAtLessThanEqualOrderByIdAsc(LocalDateTime.now())
                        .forEach(recoveryService::retry));
    }
}
