package com.backend.orbitflow.domain.payment.entity;

import com.backend.orbitflow.domain.payment.enums.PaymentStatus;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 포인트 충전 결제, 상태 전이는 결제 행 비관적 락 아래에서만 수행
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    // PG 결제 키 (승인 시 저장)
    @Column(name = "payment_key")
    private String paymentKey;

    // 결제 금액 (KRW)
    @Column(nullable = false)
    private int amount;

    // 적립 포인트
    @Column(nullable = false)
    private int point;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    public static Payment of(User user, String orderId, int amount, int point) {
        return new Payment(
                null, user, orderId, null, amount, point, PaymentStatus.READY
        );
    }

    public boolean isOwnedBy(User user) {
        return this.user.getId().equals(user.getId());
    }

    public boolean isStatus(PaymentStatus status) {
        return this.status == status;
    }

    public void approve(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.APPROVED;
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    public void refund() {
        this.status = PaymentStatus.REFUNDED;
    }
}
