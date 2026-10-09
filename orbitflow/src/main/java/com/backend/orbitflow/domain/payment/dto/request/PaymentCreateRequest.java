package com.backend.orbitflow.domain.payment.dto.request;

import com.backend.orbitflow.domain.payment.enums.PointPackage;
import jakarta.validation.constraints.NotNull;

public record PaymentCreateRequest(
        @NotNull(message = "충전 상품은 비어있을 수 없습니다.")
        PointPackage pointPackage
) {
}
