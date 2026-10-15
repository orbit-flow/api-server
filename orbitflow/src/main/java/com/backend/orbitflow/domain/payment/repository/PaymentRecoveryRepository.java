package com.backend.orbitflow.domain.payment.repository;

import com.backend.orbitflow.domain.payment.entity.PaymentRecovery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRecoveryRepository extends JpaRepository<PaymentRecovery, Long> {
    Optional<PaymentRecovery> findByOrderIdAndResolvedFalse(String orderId);
    List<PaymentRecovery> findTop100ByResolvedFalseAndNextAttemptAtLessThanEqualOrderByIdAsc(LocalDateTime now);
}
