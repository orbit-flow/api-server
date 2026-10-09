package com.backend.orbitflow.domain.chat.service;

import com.backend.orbitflow.domain.chat.dto.response.MessageResponse;
import com.backend.orbitflow.domain.chat.enums.MessageDeleteScope;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;

public interface MessageService {

    int MAX_PAGE_SIZE = 100;

    MessageResponse send(User me, String chatroomUuid, String content, String clientMessageId);
    List<MessageResponse> getMessages(User me, String chatroomUuid, Long beforeId, int size);
    void delete(User me, Long messageId, MessageDeleteScope scope);
}
