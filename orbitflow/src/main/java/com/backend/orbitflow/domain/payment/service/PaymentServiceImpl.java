package com.backend.orbitflow.domain.payment.service;

import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.payment.dto.response.PaymentResponse;
import com.backend.orbitflow.domain.payment.entity.Payment;
import com.backend.orbitflow.domain.payment.enums.PaymentStatus;
import com.backend.orbitflow.domain.payment.enums.PointPackage;
import com.backend.orbitflow.domain.payment.error.PaymentErrorCode;
import com.backend.orbitflow.domain.payment.gateway.PaymentGateway;
import com.backend.orbitflow.domain.payment.gateway.PaymentGatewayException;
import com.backend.orbitflow.domain.payment.repository.PaymentRepository;
import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.point.entity.PointTransaction;
import com.backend.orbitflow.domain.point.enums.PointTransactionType;
import com.backend.orbitflow.domain.point.service.PointLedger;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.error.ErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
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

    private static final int MAX_READY_PAYMENTS = 5;
    // 결제창을 닫아 승인되지 않은 주문이 영구히 제한에 걸리지 않도록 최근 주문만 셈
    private static final long READY_COUNT_WINDOW_MINUTES = 30;

    private final PaymentRepository paymentRepository;
    private final PointLedger pointLedger;
    private final UserService userService;
    private final PaymentGateway paymentGateway;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PointLedger pointLedger,
            UserService userService,
            PaymentGateway paymentGateway,
            PlatformTransactionManager transactionManager,
            ApplicationEventPublisher eventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.pointLedger = pointLedger;
        this.userService = userService;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
        // 포인트 변경 : READ COMMITTED 필수 (PointLedger 참고)
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    // 결제 금액·적립 포인트는 서버가 상품으로 결정, 최근 30분 안에 만든 승인 대기(READY) 주문은 사용자당 최대 5건
    // 사용자 행 락으로 동시 주문 생성을 직렬화 (락 이후 조회가 최신 커밋을 보도록 READ_COMMITTED 필수)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse createPayment(User me, PointPackage pointPackage) {
        userService.lockUser(me.getId());
        LocalDateTime since = LocalDateTime.now().minusMinutes(READY_COUNT_WINDOW_MINUTES);
        if (paymentRepository.countByUserAndStatusAndCreatedAtAfter(me, PaymentStatus.READY, since) >= MAX_READY_PAYMENTS) {
            throw new CommonException(PaymentErrorCode.TOO_MANY_READY_PAYMENTS);
        }
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
                // 일시적 오류는 승인 여부를 알 수 없으므로 주문을 READY로 유지하고 재시도 허용 (롤백)
                if (e.isRetryable()) {
                    log.warn("결제 승인 일시 오류 : orderId={}, reason={}", orderId, e.getMessage());
                    throw new CommonException(PaymentErrorCode.PAYMENT_GATEWAY_UNAVAILABLE);
                }
                log.warn("결제 승인 실패 : orderId={}, reason={}", orderId, e.getMessage());
                payment.fail();
                return ConfirmResult.failure(PaymentErrorCode.PAYMENT_CONFIRM_FAILED, PaymentResponse.from(payment));
            }

            // PG 승인 이후 DB 반영이 실패하면 결제 취소로 보상 (돈만 나가고 포인트가 없는 상태 방지)
            cancelOnRollback(paymentKey);
            payment.approve(paymentKey);
            PointTransaction transaction = pointLedger.deposit(me, PointTransactionType.PURCHASE, payment.getPoint());
            // 새로 승인된 경우에만 알림 (같은 결제의 승인 재요청은 위에서 반환), 커밋 후 발송
            eventPublisher.publishEvent(NotificationRequest.to(me, NotificationType.POINT_EARNED, null, null, null,
                    "포인트 충전 " + payment.getPoint() + "P가 적립되었습니다. (잔액 " + transaction.getBalanceAfter() + "P)"));
            return ConfirmResult.success(PaymentResponse.of(payment, transaction.getBalanceAfter()));
        });

        if (result.errorCode() != null) {
            throw new CommonException(result.errorCode(), result.response());
        }
        return result.response();
    }

    // 충전 이후 포인트를 사용하지 않았을 때만 환불 (보너스·출석 포인트로 잔액이 충분해도 사용 이력이 있으면 불가), 환불은 거래(REFUND)로 기록
    public PaymentResponse refund(User me, String orderId, String reason) {
        return transactionTemplate.execute(status -> {
            Payment payment = lockOwnedPayment(me, orderId);
            if (!payment.isStatus(PaymentStatus.APPROVED)) {
                throw new CommonException(PaymentErrorCode.PAYMENT_NOT_REFUNDABLE);
            }

            // 아바타 락 이후에 사용 이력을 확인해 동시에 진행되는 구매를 놓치지 않음
            // 승인된 결제 행은 환불 전까지 수정되지 않으므로 updatedAt이 승인 시각
            Avatar locked = pointLedger.lock(me);
            if (pointLedger.hasUsedSince(me, payment.getUpdatedAt())) {
                throw new CommonException(PaymentErrorCode.PAYMENT_POINT_ALREADY_USED);
            }
            // 포인트 차감을 먼저 수행 : 잔액 부족이면 PG 취소 전에 거부
            // PG 취소 중 아바타 락을 유지하는 이유 : 취소 후 차감하면 그 사이 구매로 잔액이 부족해져 돈만 환불되고 포인트가 남을 수 있음
            PointTransaction transaction = pointLedger.withdraw(locked, me, PointTransactionType.REFUND, payment.getPoint());
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
        return paymentRepository.findAllByUserOrderByCreatedAtDescIdDesc(me, PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100)))
                .map(PaymentResponse::from);
    }

    // 다른 사용자의 주문은 존재 여부를 노출하지 않도록 NOT_FOUND 처리 (소유자 조건은 쿼리에서 적용)
    private Payment lockOwnedPayment(User me, String orderId) {
        return paymentRepository.findByOrderIdAndUserForUpdate(orderId, me)
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
