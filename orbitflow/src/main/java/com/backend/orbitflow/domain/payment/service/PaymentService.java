package com.backend.orbitflow.domain.payment.service;

import com.backend.orbitflow.domain.payment.dto.response.PaymentResponse;
import com.backend.orbitflow.domain.payment.enums.PointPackage;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;

public interface PaymentService {

    PaymentResponse createPayment(User me, PointPackage pointPackage);
    PaymentResponse confirm(User me, String orderId, String paymentKey, int amount);
    PaymentResponse refund(User me, String orderId, String reason);
    Page<PaymentResponse> getMyPayments(User me, int page, int size);
}
