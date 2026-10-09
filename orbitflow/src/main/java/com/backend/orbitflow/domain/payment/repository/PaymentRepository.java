package com.backend.orbitflow.domain.payment.repository;

import com.backend.orbitflow.domain.payment.entity.Payment;
import com.backend.orbitflow.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 같은 주문의 동시 승인·환불 요청을 직렬화 (join fetch 없이 payments 행만 잠금)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderId = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") String orderId);

    Page<Payment> findAllByUserOrderByCreatedAtDescIdDesc(User user, Pageable pageable);
}
