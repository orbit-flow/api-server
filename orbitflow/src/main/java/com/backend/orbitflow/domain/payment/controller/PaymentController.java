package com.backend.orbitflow.domain.payment.controller;

import com.backend.orbitflow.domain.payment.dto.request.PaymentConfirmRequest;
import com.backend.orbitflow.domain.payment.dto.request.PaymentCreateRequest;
import com.backend.orbitflow.domain.payment.dto.request.PaymentRefundRequest;
import com.backend.orbitflow.domain.payment.dto.response.PaymentResponse;
import com.backend.orbitflow.domain.payment.dto.response.PaymentSuccessCode;
import com.backend.orbitflow.domain.payment.dto.response.PointPackageResponse;
import com.backend.orbitflow.domain.payment.facade.PaymentFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.RequestParam;

// 흐름 : 상품 조회 -> 주문 생성(orderId, amount) -> PG 결제창 -> 승인(confirm) -> 포인트 적립
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @GetMapping("/packages")
    public ResponseEntity<CommonResponse<PageResponse<PointPackageResponse>>> getPointPackages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PaymentSuccessCode.GET_POINT_PACKAGES,
                        paymentFacade.getPointPackages(page, size)
                ));
    }

    @PostMapping
    public ResponseEntity<CommonResponse<PaymentResponse>> createPayment(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PaymentCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        PaymentSuccessCode.PAYMENT_CREATE,
                        paymentFacade.createPayment(authUser, request)
                ));
    }

    // 같은 paymentKey로 재요청 시 기존 승인 결과 반환 (멱등)
    @PostMapping("/confirm")
    public ResponseEntity<CommonResponse<PaymentResponse>> confirm(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PaymentSuccessCode.PAYMENT_CONFIRM,
                        paymentFacade.confirm(authUser, request)
                ));
    }

    // 충전한 포인트가 남아 있을 때만 환불 가능
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<CommonResponse<PaymentResponse>> refund(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String orderId,
            @Valid @RequestBody PaymentRefundRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PaymentSuccessCode.PAYMENT_REFUND,
                        paymentFacade.refund(authUser, orderId, request)
                ));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PaymentSuccessCode.GET_PAYMENT_LIST,
                        paymentFacade.getMyPayments(authUser, page, size)
                ));
    }
}
