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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

// 승인·환불은 PaymentService의 @Transactional이 경계(실패 처리 커밋 포함)를 제어하므로 파사드에 @Transactional을 두지 않음 (주문 생성만 사용자 행 락과 같은 트랜잭션 필요)
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentService paymentService;
    private final UserService userService;

    // 상품 구성은 코드에 고정된 값이므로 페이지 없이 전체 반환
    public List<PointPackageResponse> getPointPackages() {
        return Arrays.stream(PointPackage.values())
                .map(PointPackageResponse::from)
                .toList();
    }

    // 사용자 행 락으로 동시 주문 생성을 직렬화 : 락과 주문 쓰기를 한 트랜잭션에 묶음 (READ_COMMITTED 필수)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse createPayment(AuthUser authUser, PaymentCreateRequest request) {
        User me = me(authUser);
        userService.lockUser(me.getId());
        return paymentService.createPayment(me, request.pointPackage());
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
        return userService.getActiveByUuid(authUser.getUuid());
    }
}
