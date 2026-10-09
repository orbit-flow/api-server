package com.backend.orbitflow.domain.suspension.repository;

import com.backend.orbitflow.domain.suspension.entity.Suspension;
import com.backend.orbitflow.domain.suspension.enums.SuspensionStatus;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SuspensionRepository extends JpaRepository<Suspension, Long> {

    @Query("""
            select s from Suspension s
            join fetch s.user
            join fetch s.suspendedBy
            left join fetch s.releasedBy
            where s.id = :id
            """)
    Optional<Suspension> findWithAllById(@Param("id") Long id);

    // 사용자당 진행 중인 정지는 1건
    @Query("""
            select s from Suspension s
            where s.user = :user
              and s.state = com.backend.orbitflow.domain.suspension.enums.SuspensionStatus.ACTIVE
            """)
    Optional<Suspension> findActiveByUser(@Param("user") User user);

    // 만료 시각이 지난 진행 중인 기간 정지
    @Query("""
            select s from Suspension s
            join fetch s.user
            where s.state = com.backend.orbitflow.domain.suspension.enums.SuspensionStatus.ACTIVE
              and s.expiresAt is not null
              and s.expiresAt <= :now
            """)
    List<Suspension> findAllExpired(@Param("now") LocalDateTime now);

    // 관리자 목록 : 상태 필터, 사용자 이름·이메일 검색 (null이면 전체)
    @Query(value = """
            select s from Suspension s
            join fetch s.user u
            where (:status is null or s.state = :status)
              and (:keyword is null
                   or u.name like concat('%', :keyword, '%')
                   or u.email like concat('%', :keyword, '%'))
            order by s.createdAt desc
            """,
            countQuery = """
            select count(s) from Suspension s
            join s.user u
            where (:status is null or s.state = :status)
              and (:keyword is null
                   or u.name like concat('%', :keyword, '%')
                   or u.email like concat('%', :keyword, '%'))
            """)
    Page<Suspension> search(
            @Param("status") SuspensionStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
