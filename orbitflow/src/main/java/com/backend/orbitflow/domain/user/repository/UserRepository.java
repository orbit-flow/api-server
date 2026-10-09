package com.backend.orbitflow.domain.user.repository;

import com.backend.orbitflow.domain.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.orbitflow.domain.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{

    Optional<User> findByUuidAndDeletedAtIsNullAndStatusNot(String uuid, UserStatus status);
    Optional<User> findByEmail(String email);
}
