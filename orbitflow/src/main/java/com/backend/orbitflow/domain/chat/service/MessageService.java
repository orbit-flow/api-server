package com.backend.orbitflow.domain.chat.service;

import com.backend.orbitflow.domain.chat.dto.response.MessageResponse;
import com.backend.orbitflow.domain.chat.enums.MessageDeleteScope;
import com.backend.orbitflow.domain.user.entity.User;

import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.entity.ChatroomMember;
import com.backend.orbitflow.domain.chat.entity.Message;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.Set;

// 채팅방·참여자 조회와 차단·사용자 조회는 호출 측(ChatFacade)에서 수행해 전달
public interface MessageService {

    // send 순서 : findSent(이미 보낸 메시지면 그대로 반환) → (1:1이면) getDirectPartnerId → validateDirectPartner → send
    Optional<MessageResponse> findSent(User me, Chatroom chatroom, String clientMessageId);
    Long getDirectPartnerId(Chatroom chatroom, User me);
    User validateDirectPartner(Optional<User> partner, boolean blocked);
    MessageResponse send(User me, Chatroom chatroom, ChatroomMember member, String content, String clientMessageId);
    Page<MessageResponse> getMessages(User me, Chatroom chatroom, ChatroomMember member, Set<Long> blockeeIds, Long beforeId, int page, int size);
    Message getMessage(Long messageId);
    void delete(User me, Message message, ChatroomMember member, MessageDeleteScope scope);
}
