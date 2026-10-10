package com.backend.orbitflow.domain.payment.repository;

import com.backend.orbitflow.domain.payment.entity.Payment;
import com.backend.orbitflow.domain.payment.enums.PaymentStatus;
import com.backend.orbitflow.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 같은 주문의 동시 승인·환불 요청을 직렬화 (join fetch 없이 payments 행만 잠금)
    // 소유자 조건을 쿼리에 포함해 다른 사용자의 주문 행은 잠그지 않음
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderId = :orderId and p.user = :user")
    Optional<Payment> findByOrderIdAndUserForUpdate(@Param("orderId") String orderId, @Param("user") User user);

    // 사용자의 상태별 주문 중 since 이후 생성된 수 (승인 대기 주문 수 제한용)
    long countByUserAndStatusAndCreatedAtAfter(User user, PaymentStatus status, LocalDateTime since);

    Page<Payment> findAllByUserOrderByCreatedAtDescIdDesc(User user, Pageable pageable);
}
