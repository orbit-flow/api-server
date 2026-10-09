package com.backend.orbitflow.domain.point.repository;

import com.backend.orbitflow.domain.point.entity.PointTransaction;
import com.backend.orbitflow.domain.point.enums.PointTransactionType;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    // [from, to) 구간에 해당 유형의 거래가 있는지 (출석 1일 1회 판정용)
    boolean existsByUserAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            User user, PointTransactionType type, LocalDateTime from, LocalDateTime to
    );

    boolean existsBySourceTransaction(PointTransaction sourceTransaction);

    @Query("select t from PointTransaction t join fetch t.user where t.id = :id")
    Optional<PointTransaction> findWithUserById(@Param("id") Long id);

    // 거래 내역 (type 미지정 시 전체, 최신순)
    @Query(value = """
            select t from PointTransaction t
            where t.user = :user
              and (:type is null or t.type = :type)
            order by t.createdAt desc, t.id desc
            """,
            countQuery = """
            select count(t) from PointTransaction t
            where t.user = :user
              and (:type is null or t.type = :type)
            """)
    Page<PointTransaction> search(@Param("user") User user, @Param("type") PointTransactionType type, Pageable pageable);
}
