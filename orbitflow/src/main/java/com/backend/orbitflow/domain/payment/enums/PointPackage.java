package com.backend.orbitflow.domain.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 포인트 충전 상품 (결제 금액은 서버가 결정하며, 클라이언트가 보낸 금액은 검증용으로만 사용)
@Getter
@RequiredArgsConstructor
public enum PointPackage {

    POINT_1000(1_000, 1_000),
    POINT_5000(5_000, 5_000),
    POINT_10000(10_000, 10_000),
    POINT_30000(30_000, 30_000);

    // 결제 금액 (KRW)
    private final int amount;
    // 적립 포인트
    private final int point;
}
