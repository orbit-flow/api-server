package com.backend.orbitflow.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// PG 결제창 완료 후 전달받은 값으로 승인 요청
public record PaymentConfirmRequest(
        @NotBlank(message = "주문 id는 비어있을 수 없습니다.")
        String orderId,

        @NotBlank(message = "결제 키는 비어있을 수 없습니다.")
        String paymentKey,

        // 위·변조 검증용 (주문 생성 시 서버가 정한 금액과 비교)
        @NotNull(message = "결제 금액은 비어있을 수 없습니다.")
        Integer amount
) {
}
