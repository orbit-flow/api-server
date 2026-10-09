package com.backend.orbitflow.domain.avatar.repository;

import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AvatarRepository extends JpaRepository<Avatar, Long> {

    Optional<Avatar> findByUser(User user);

    // 아바타가 없는 사용자 (기능 도입 이전 가입자 보정용)
    @Query("select u from User u where not exists (select a.id from Avatar a where a.user = u)")
    List<User> findUsersWithoutAvatar();
}
