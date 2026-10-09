package com.backend.orbitflow.domain.avatar.repository;

import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AvatarRepository extends JpaRepository<Avatar, Long> {

    Optional<Avatar> findByUser(User user);

    // 포인트 변경 시 동시 요청(중복 출석·동시 구매) 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Avatar a where a.user = :user")
    Optional<Avatar> findByUserForUpdate(@Param("user") User user);
}
