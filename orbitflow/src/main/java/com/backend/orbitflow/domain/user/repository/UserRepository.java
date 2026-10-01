package com.backend.orbitflow.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.orbitflow.domain.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{

    Optional<User> findByUuid(String uuid);
    Optional<User> findByEmail(String email);
}
