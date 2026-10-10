package com.backend.orbitflow.domain.payment.gateway;

public class PaymentGatewayException extends RuntimeException {

    // 일시적 오류(타임아웃·PG 장애 등) 여부 : true면 승인 여부를 알 수 없으므로 주문을 실패 처리하지 않고 재시도 허용
    // false(기본)는 PG의 확정 거절
    private final boolean retryable;

    public PaymentGatewayException(String message) {
        this(message, false);
    }

    public PaymentGatewayException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public PaymentGatewayException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public PaymentGatewayException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
