package com.backend.orbitflow.domain.payment.facade;

import com.backend.orbitflow.domain.payment.dto.request.PaymentConfirmRequest;
import com.backend.orbitflow.domain.payment.dto.request.PaymentCreateRequest;
import com.backend.orbitflow.domain.payment.dto.request.PaymentRefundRequest;
import com.backend.orbitflow.domain.payment.dto.response.PaymentResponse;
import com.backend.orbitflow.domain.payment.dto.response.PointPackageResponse;
import com.backend.orbitflow.domain.payment.enums.PointPackage;
import com.backend.orbitflow.domain.payment.service.PaymentService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

// 승인·환불은 PaymentService가 트랜잭션 경계를 직접 제어하므로 파사드에 @Transactional을 두지 않음
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentService paymentService;
    private final UserService userService;

    // 상품 구성은 코드에 고정된 값이므로 메모리 목록을 같은 페이지 형식으로 반환
    public PageResponse<PointPackageResponse> getPointPackages(int page, int size) {
        return PageResponse.of(Arrays.stream(PointPackage.values())
                .map(PointPackageResponse::from)
                .toList(), page, size);
    }

    public PaymentResponse createPayment(AuthUser authUser, PaymentCreateRequest request) {
        return paymentService.createPayment(me(authUser), request.pointPackage());
    }

    public PaymentResponse confirm(AuthUser authUser, PaymentConfirmRequest request) {
        return paymentService.confirm(me(authUser), request.orderId(), request.paymentKey(), request.amount());
    }

    public PaymentResponse refund(AuthUser authUser, String orderId, PaymentRefundRequest request) {
        return paymentService.refund(me(authUser), orderId, request.reason());
    }

    public PageResponse<PaymentResponse> getMyPayments(AuthUser authUser, int page, int size) {
        return PageResponse.from(paymentService.getMyPayments(me(authUser), page, size));
    }

    private User me(AuthUser authUser) {
        return userService.getByUuid(authUser.getUuid());
    }
}
