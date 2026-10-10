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

    // from 이후 해당 유형의 거래가 있는지 (충전 이후 포인트 사용 여부 판정용)
    boolean existsByUserAndTypeAndCreatedAtGreaterThanEqual(User user, PointTransactionType type, LocalDateTime from);

    boolean existsBySourceTransaction(PointTransaction sourceTransaction);

    @Query("select t from PointTransaction t join fetch t.user where t.id = :id")
    Optional<PointTransaction> findWithUserById(@Param("id") Long id);

    // 거래 내역 (최신순) : 유형 지정 여부에 따라 쿼리를 나눠 각각 (user_id, type, created_at), (user_id, created_at) 인덱스 사용
    // (:type is null or ...) 형태는 실행 계획이 한 번에 정해져 유형 미지정 시에도 정렬 인덱스를 쓰지 못함
    @Query(value = """
            select t from PointTransaction t
            where t.user = :user
            order by t.createdAt desc, t.id desc
            """,
            countQuery = """
            select count(t) from PointTransaction t
            where t.user = :user
            """)
    Page<PointTransaction> searchAll(@Param("user") User user, Pageable pageable);

    @Query(value = """
            select t from PointTransaction t
            where t.user = :user
              and t.type = :type
            order by t.createdAt desc, t.id desc
            """,
            countQuery = """
            select count(t) from PointTransaction t
            where t.user = :user
              and t.type = :type
            """)
    Page<PointTransaction> search(@Param("user") User user, @Param("type") PointTransactionType type, Pageable pageable);
}
