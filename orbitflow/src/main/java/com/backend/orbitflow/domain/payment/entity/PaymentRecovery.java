package com.backend.orbitflow.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment_recoveries")
public class PaymentRecovery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(nullable = false, length = 128)
    private String paymentKey;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(nullable = false)
    private boolean resolved;

    private LocalDateTime resolvedAt;

    private PaymentRecovery(String orderId, String paymentKey) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.nextAttemptAt = LocalDateTime.now();
    }

    public static PaymentRecovery of(String orderId, String paymentKey) {
        return new PaymentRecovery(orderId, paymentKey);
    }

    public void retryLater() {
        attempts++;
        nextAttemptAt = LocalDateTime.now().plusMinutes(Math.min(60, Math.max(1, attempts * 5)));
    }

    public void resolve() {
        resolved = true;
        resolvedAt = LocalDateTime.now();
    }
}
