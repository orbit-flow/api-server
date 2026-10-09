package com.backend.orbitflow.domain.payment.service;

import com.backend.orbitflow.domain.notification.event.PointEarnedEvent;
import com.backend.orbitflow.domain.payment.dto.response.PaymentResponse;
import com.backend.orbitflow.domain.payment.entity.Payment;
import com.backend.orbitflow.domain.payment.enums.PaymentStatus;
import com.backend.orbitflow.domain.payment.enums.PointPackage;
import com.backend.orbitflow.domain.payment.error.PaymentErrorCode;
import com.backend.orbitflow.domain.payment.gateway.PaymentGateway;
import com.backend.orbitflow.domain.payment.gateway.PaymentGatewayException;
import com.backend.orbitflow.domain.payment.repository.PaymentRepository;
import com.backend.orbitflow.domain.point.entity.PointTransaction;
import com.backend.orbitflow.domain.point.enums.PointTransactionType;
import com.backend.orbitflow.domain.point.service.PointLedger;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.ErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * 포인트 충전 결제
 *
 * <p>동시성 규칙
 * <ul>
 *   <li>승인·환불은 결제 행 락(payments FOR UPDATE)을 먼저 잡고, 포인트 변경 시 아바타 행 락을 잡음 (락 순서 고정)</li>
 *   <li>상태 전이(READY -> APPROVED -> REFUNDED)는 락 아래에서만 수행되어 같은 주문의 중복 적립·중복 환불이 불가</li>
 *   <li>같은 paymentKey로 승인을 재요청하면(네트워크 재시도 등) 기존 승인 결과를 그대로 반환 (멱등)</li>
 *   <li>PG 호출 동안 결제 행 락을 보유하므로 PG 구현체는 짧은 타임아웃 필요</li>
 * </ul>
 *
 * <p>승인 실패 시 FAILED 상태는 커밋하고 에러를 반환해야 하므로 TransactionTemplate으로 트랜잭션 경계를 직접 제어
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PointLedger pointLedger;
    private final PaymentGateway paymentGateway;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PointLedger pointLedger,
            PaymentGateway paymentGateway,
            PlatformTransactionManager transactionManager,
            ApplicationEventPublisher eventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.pointLedger = pointLedger;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
        // 포인트 변경 : READ COMMITTED 필수 (PointLedger 참고)
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    // 결제 금액·적립 포인트는 서버가 상품으로 결정
    @Transactional
    public PaymentResponse createPayment(User me, PointPackage pointPackage) {
        Payment payment = paymentRepository.save(Payment.of(
                me,
                UUID.randomUUID().toString(),
                pointPackage.getAmount(),
                pointPackage.getPoint()
        ));
        return PaymentResponse.from(payment);
    }

    // 결제 승인 시점에 포인트를 적립하고 거래(PURCHASE)로 기록
    public PaymentResponse confirm(User me, String orderId, String paymentKey, int amount) {
        ConfirmResult result = transactionTemplate.execute(status -> {
            Payment payment = lockOwnedPayment(me, orderId);

            // 같은 결제의 승인 재요청 : 기존 결과 반환
            if (payment.isStatus(PaymentStatus.APPROVED) && paymentKey.equals(payment.getPaymentKey())) {
                return ConfirmResult.success(PaymentResponse.from(payment));
            }
            if (!payment.isStatus(PaymentStatus.READY)) {
                throw new CommonException(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED);
            }
            // 금액 위·변조 : 주문을 실패 처리하고 PG 승인은 호출하지 않음
            if (payment.getAmount() != amount) {
                payment.fail();
                return ConfirmResult.failure(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH, PaymentResponse.from(payment));
            }
            try {
                paymentGateway.confirm(paymentKey, orderId, amount);
            } catch (PaymentGatewayException e) {
                log.warn("결제 승인 실패 : orderId={}, reason={}", orderId, e.getMessage());
                payment.fail();
                return ConfirmResult.failure(PaymentErrorCode.PAYMENT_CONFIRM_FAILED, PaymentResponse.from(payment));
            }

            // PG 승인 이후 DB 반영이 실패하면 결제 취소로 보상 (돈만 나가고 포인트가 없는 상태 방지)
            cancelOnRollback(paymentKey);
            payment.approve(paymentKey);
            PointTransaction transaction = pointLedger.deposit(me, PointTransactionType.PURCHASE, payment.getPoint());
            eventPublisher.publishEvent(new PointEarnedEvent(me.getId(), "포인트 충전", payment.getPoint(), transaction.getBalanceAfter()));
            return ConfirmResult.success(PaymentResponse.of(payment, transaction.getBalanceAfter()));
        });

        if (result.errorCode() != null) {
            throw new CommonException(result.errorCode(), result.response());
        }
        return result.response();
    }

    // 충전한 포인트가 남아 있을 때만 환불 (이미 사용한 포인트는 환불 불가), 환불은 거래(REFUND)로 기록
    public PaymentResponse refund(User me, String orderId, String reason) {
        return transactionTemplate.execute(status -> {
            Payment payment = lockOwnedPayment(me, orderId);
            if (!payment.isStatus(PaymentStatus.APPROVED)) {
                throw new CommonException(PaymentErrorCode.PAYMENT_NOT_REFUNDABLE);
            }

            // 포인트 차감을 먼저 수행 : 잔액 부족이면 PG 취소 전에 거부
            PointTransaction transaction = pointLedger.withdraw(me, PointTransactionType.REFUND, payment.getPoint());
            try {
                paymentGateway.cancel(payment.getPaymentKey(), reason);
            } catch (PaymentGatewayException e) {
                // 예외로 롤백되어 포인트 차감도 반영되지 않음
                log.warn("결제 취소 실패 : orderId={}, reason={}", orderId, e.getMessage());
                throw new CommonException(PaymentErrorCode.PAYMENT_CANCEL_FAILED);
            }
            alertOnRollbackAfterCancel(orderId);
            payment.refund();
            return PaymentResponse.of(payment, transaction.getBalanceAfter());
        });
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(User me, int page, int size) {
        return paymentRepository.findAllByUserOrderByCreatedAtDescIdDesc(me, PageRequest.of(Math.max(page - 1, 0), size))
                .map(PaymentResponse::from);
    }

    // 다른 사용자의 주문은 존재 여부를 노출하지 않도록 NOT_FOUND 처리
    private Payment lockOwnedPayment(User me, String orderId) {
        return paymentRepository.findByOrderIdForUpdate(orderId)
                .filter(payment -> payment.isOwnedBy(me))
                .orElseThrow(() -> new CommonException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    private void cancelOnRollback(String paymentKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    return;
                }
                try {
                    paymentGateway.cancel(paymentKey, "포인트 적립 처리 실패로 인한 자동 취소");
                } catch (PaymentGatewayException e) {
                    log.error("[결제 정합성 확인 필요] 승인 후 DB 반영 실패, 자동 취소도 실패 : paymentKey={}", paymentKey, e);
                }
            }
        });
    }

    // PG 취소 후 DB 반영이 실패하면 PG와 DB가 어긋나므로 수동 확인용 로그
    private void alertOnRollbackAfterCancel(String orderId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    log.error("[결제 정합성 확인 필요] PG 취소 완료 후 DB 반영 실패 : orderId={}", orderId);
                }
            }
        });
    }

    private record ConfirmResult(PaymentResponse response, ErrorCode errorCode) {

        static ConfirmResult success(PaymentResponse response) {
            return new ConfirmResult(response, null);
        }

        static ConfirmResult failure(ErrorCode errorCode, PaymentResponse response) {
            return new ConfirmResult(response, errorCode);
        }
    }
}
