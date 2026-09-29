package com.backend.orbitflow.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.orbitflow.domain.user.entity.User;

public interface UserRepository extends JpaRepository<Long, User>{

}
