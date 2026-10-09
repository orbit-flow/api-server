package com.backend.orbitflow.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentRefundRequest(
        @NotBlank(message = "환불 사유는 비어있을 수 없습니다.")
        @Size(max = 200, message = "환불 사유는 200자 이내여야 합니다.")
        String reason
) {
}
