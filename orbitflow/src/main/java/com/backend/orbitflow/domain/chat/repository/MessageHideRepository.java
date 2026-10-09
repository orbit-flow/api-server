package com.backend.orbitflow.domain.chat.repository;

import com.backend.orbitflow.domain.chat.entity.Message;
import com.backend.orbitflow.domain.chat.entity.MessageHide;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageHideRepository extends JpaRepository<MessageHide, Long> {

    boolean existsByMessageAndUser(Message message, User user);
}
