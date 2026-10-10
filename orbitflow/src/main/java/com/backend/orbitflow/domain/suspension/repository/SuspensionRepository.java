package com.backend.orbitflow.domain.suspension.repository;

import com.backend.orbitflow.domain.suspension.entity.Suspension;
import com.backend.orbitflow.domain.suspension.enums.SuspensionStatus;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.backend.orbitflow.domain.suspension.dto.response.SuspensionListResponse;

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

    // 만료 시각이 지난 진행 중인 기간 정지 (일괄 만료 처리용 : 엔티티 대신 필요한 식별자만 조회)
    @Query("""
            select s.id as id, u.id as userId, u.uuid as userUuid
            from Suspension s
            join s.user u
            where s.state = com.backend.orbitflow.domain.suspension.enums.SuspensionStatus.ACTIVE
              and s.expiresAt is not null
              and s.expiresAt <= :now
            """)
    List<ExpiredSuspension> findAllExpired(@Param("now") LocalDateTime now);

    // 일괄 만료 : 그사이 해제된 정지는 덮어쓰지 않도록 진행 중인 정지만 변경
    // 벌크 JPQL은 영속성 컨텍스트와 감사(@LastModifiedDate)를 거치지 않으므로 updatedAt을 직접 지정
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Suspension s
            set s.state = com.backend.orbitflow.domain.suspension.enums.SuspensionStatus.EXPIRED,
                s.updatedAt = :now
            where s.id in :ids
              and s.state = com.backend.orbitflow.domain.suspension.enums.SuspensionStatus.ACTIVE
            """)
    int expireAllByIdIn(@Param("ids") Collection<Long> ids, @Param("now") LocalDateTime now);

    // 만료된 정지 대상 계정 일괄 복구 : 다른 진행 중인 정지가 있는 계정(그사이 재정지)은 정지 상태 유지
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update User u
            set u.status = com.backend.orbitflow.domain.user.enums.UserStatus.ACTIVE,
                u.updatedAt = :now
            where u.id in :userIds
              and u.status = com.backend.orbitflow.domain.user.enums.UserStatus.BANNED
              and not exists (
                  select s.id from Suspension s
                  where s.user.id = u.id
                    and s.state = com.backend.orbitflow.domain.suspension.enums.SuspensionStatus.ACTIVE
              )
            """)
    int restoreUsersByIdIn(@Param("userIds") Collection<Long> userIds, @Param("now") LocalDateTime now);

    interface ExpiredSuspension {
        Long getId();
        Long getUserId();
        String getUserUuid();
    }

    // 관리자 목록 : 상태 필터, 사용자 이름·이메일 검색 (null이면 전체), 목록 응답으로 바로 반환
    // keyword는 서비스에서 %, _, !를 '!'로 이스케이프해 전달 (와일드카드가 아닌 문자 그대로 검색)
    @Query(value = """
            select new com.backend.orbitflow.domain.suspension.dto.response.SuspensionListResponse(
                s.id, u.uuid, u.name, u.email, s.state, s.reason, s.expiresAt, s.createdAt)
            from Suspension s
            join s.user u
            where (:status is null or s.state = :status)
              and (:keyword is null
                   or u.name like concat('%', :keyword, '%') escape '!'
                   or u.email like concat('%', :keyword, '%') escape '!')
            order by s.createdAt desc
            """,
            countQuery = """
            select count(s) from Suspension s
            join s.user u
            where (:status is null or s.state = :status)
              and (:keyword is null
                   or u.name like concat('%', :keyword, '%') escape '!'
                   or u.email like concat('%', :keyword, '%') escape '!')
            """)
    Page<SuspensionListResponse> search(
            @Param("status") SuspensionStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
