package com.backend.orbitflow.domain.point.entity;

import com.backend.orbitflow.domain.point.enums.PointTransactionType;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 처리에 성공한 포인트 거래만 기록 (amount : 적립은 양수, 차감·회수는 음수)
// 모든 거래는 PointLedger가 아바타 행 락을 잡은 상태에서만 생성
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "point_transactions",
        indexes = {
                // 유형별 거래 내역, 오늘 출석 여부
                @Index(columnList = "user_id, type, created_at"),
                // 전체 거래 내역 (유형 미지정, 최신순)
                @Index(columnList = "user_id, created_at")
        })
public class PointTransaction extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionType type;

    @Column(nullable = false)
    private int amount;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    // ERD 확장 : REVOKE 거래가 가리키는 원본 거래 (unique로 같은 거래의 중복 회수를 DB 수준에서 차단)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_transaction_id", unique = true)
    private PointTransaction sourceTransaction;

    public static PointTransaction of(User user, PointTransactionType type, int amount, int balanceAfter, PointTransaction sourceTransaction) {
        return new PointTransaction(
                null, user, type, amount, balanceAfter, sourceTransaction
        );
    }
}
