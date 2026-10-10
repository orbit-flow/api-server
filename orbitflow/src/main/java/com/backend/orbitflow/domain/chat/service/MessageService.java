package com.backend.orbitflow.domain.chat.service;

import com.backend.orbitflow.domain.chat.dto.response.MessageResponse;
import com.backend.orbitflow.domain.chat.enums.MessageDeleteScope;
import com.backend.orbitflow.domain.user.entity.User;

import org.springframework.data.domain.Page;

public interface MessageService {

    MessageResponse send(User me, String chatroomUuid, String content, String clientMessageId);
    Page<MessageResponse> getMessages(User me, String chatroomUuid, Long beforeId, int page, int size);
    void delete(User me, Long messageId, MessageDeleteScope scope);
}
