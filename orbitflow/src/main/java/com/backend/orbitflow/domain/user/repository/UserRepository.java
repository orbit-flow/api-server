package com.backend.orbitflow.domain.user.repository;

import com.backend.orbitflow.domain.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.backend.orbitflow.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{

    Optional<User> findByUuidAndDeletedAtIsNullAndStatusNot(String uuid, UserStatus status);
    Optional<User> findByUuidAndDeletedAtIsNull(String uuid);
    Optional<User> findByEmail(String email);

    // 마지막 로그인(로그인 기록이 없으면 가입 시각)으로부터 기준 시각이 지난 활성 계정을 휴면 전환
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update User u set u.status = com.backend.orbitflow.domain.user.enums.UserStatus.SLEEP
            where u.status = com.backend.orbitflow.domain.user.enums.UserStatus.ACTIVE
              and u.deletedAt is null
              and ((u.lastLoginAt is not null and u.lastLoginAt < :threshold)
                   or (u.lastLoginAt is null and u.createdAt < :threshold))
            """)
    int convertToDormant(@Param("threshold") LocalDateTime threshold);
}
