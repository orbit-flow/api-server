package com.backend.orbitflow.domain.avatar.repository;

import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AvatarRepository extends JpaRepository<Avatar, Long> {

    Optional<Avatar> findByUser(User user);

}
