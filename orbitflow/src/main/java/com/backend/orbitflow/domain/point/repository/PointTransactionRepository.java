package com.backend.orbitflow.domain.point.repository;

import com.backend.orbitflow.domain.point.entity.PointTransaction;
import com.backend.orbitflow.domain.point.enums.PointTransactionType;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    // [from, to) 구간에 해당 유형의 거래가 있는지 (출석 1일 1회 판정용)
    boolean existsByUserAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            User user, PointTransactionType type, LocalDateTime from, LocalDateTime to
    );

    Page<PointTransaction> findAllByUserOrderByCreatedAtDescIdDesc(User user, Pageable pageable);
}
