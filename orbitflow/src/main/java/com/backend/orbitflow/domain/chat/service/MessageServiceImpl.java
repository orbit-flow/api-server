package com.backend.orbitflow.domain.chat.service;

import com.backend.orbitflow.domain.chat.dto.response.ChatEvent;
import com.backend.orbitflow.domain.chat.dto.response.MessageResponse;
import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.entity.ChatroomMember;
import com.backend.orbitflow.domain.chat.entity.Message;
import com.backend.orbitflow.domain.chat.entity.MessageHide;
import com.backend.orbitflow.domain.chat.enums.MessageDeleteScope;
import com.backend.orbitflow.domain.chat.error.ChatErrorCode;
import com.backend.orbitflow.domain.chat.repository.MessageHideRepository;
import com.backend.orbitflow.domain.chat.repository.MessageRepository;
import com.backend.orbitflow.domain.chat.websocket.ChatPublisher;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.UserStatus;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageHideRepository messageHideRepository;
    private final ChatPublisher chatPublisher;

    // 같은 clientMessageId로 재요청하면 새로 저장하지 않고 기존 메시지 반환 (호출 측이 있으면 바로 응답)
    @Transactional(readOnly = true)
    public Optional<MessageResponse> findSent(User me, Chatroom chatroom, String clientMessageId) {
        if (clientMessageId == null) {
            return Optional.empty();
        }
        return messageRepository.findByChatroomAndSenderAndClientMessageId(chatroom, me, clientMessageId)
                .map(message -> MessageResponse.of(message, chatroom.getUuid(), false));
    }

    // 응답이 곧 전송 완료 확인 (클라이언트는 응답 전까지 전송 중 상태 표시)
    // 실패한 전송은 클라이언트가 실패 상태로 유지하고, 사용자가 재전송을 요청할 때만 새 clientMessageId로 다시 전송
    // 채팅방·참여자 확인과 1:1 상대 처리(차단·참여 복구)는 호출 측(ChatFacade)이 findSent 이후 순서대로 수행
    public MessageResponse send(User me, Chatroom chatroom, ChatroomMember member, String content, String clientMessageId) {
        String chatroomUuid = chatroom.getUuid();
        Message message = messageRepository.save(Message.of(chatroom, me, content, clientMessageId));
        member.read();
        MessageResponse response = MessageResponse.of(message, chatroomUuid, false);
        chatPublisher.publishAfterCommit(chatroomUuid, new ChatEvent(ChatEvent.Type.MESSAGE, response));
        return response;
    }

    // 참여 이후 메시지만 최신순 (beforeId 미지정 시 가장 최근부터), 내가 차단한 사용자의 메시지는 blockedSender 표시
    @Transactional(readOnly = true)
    // 최신순 페이지, beforeId를 주면 그보다 오래된 메시지만 (첫 조회 시점의 최신 id를 고정해 넘기면 새 메시지로 페이지가 밀리지 않음)
    public Page<MessageResponse> getMessages(User me, Chatroom chatroom, ChatroomMember member, Set<Long> blockeeIds, Long beforeId, int page, int size) {
        Pageable pageable = toPageable(page, size);
        String chatroomUuid = chatroom.getUuid();
        return messageRepository.findVisibleMessages(
                chatroom, me, member.joinedAt(), beforeId == null ? Long.MAX_VALUE : beforeId, pageable
        ).map(message -> MessageResponse.of(message, chatroomUuid, blockeeIds.contains(message.getSender().getId())));
    }

    @Transactional(readOnly = true)
    public Message getMessage(Long messageId) {
        return messageRepository.findWithAllById(messageId).orElseThrow(
                () -> new CommonException(ChatErrorCode.MESSAGE_NOT_FOUND)
        );
    }

    // ME : 내 화면에서만 삭제 / ALL : 작성자가 모든 참여자 대상 전송 취소 (즉시 적용)
    // member : 메시지가 속한 채팅방의 내 참여 정보 (호출 측이 조회)
    public void delete(User me, Message message, ChatroomMember member, MessageDeleteScope scope) {
        Chatroom chatroom = message.getChatroom();
        // 참여 이전 메시지는 열람할 수 없으므로 존재하지 않는 것으로 처리
        if (message.getCreatedAt().isBefore(member.joinedAt())) {
            throw new CommonException(ChatErrorCode.MESSAGE_NOT_FOUND);
        }

        if (scope == MessageDeleteScope.ME) {
            if (!messageHideRepository.existsByMessageAndUser(message, me)) {
                messageHideRepository.save(MessageHide.of(message, me));
            }
            return;
        }
        if (!message.isSender(me)) {
            throw new CommonException(ChatErrorCode.NOT_MESSAGE_SENDER);
        }
        if (message.isUnsent()) {
            throw new CommonException(ChatErrorCode.ALREADY_UNSENT);
        }
        message.unsend();
        chatPublisher.publishAfterCommit(chatroom.getUuid(),
                new ChatEvent(ChatEvent.Type.UNSENT, MessageResponse.of(message, chatroom.getUuid(), false)));
    }

    // 1:1 대화 : 차단 관계가 성립하면 기존 대화에서도 새 메시지 전송 불가 (차단 우선)
    // 상대가 대화에서 나갔으면 다시 참여시킴 (상대는 이 메시지부터 열람) : 호출 측이 반환된 상대로 참여 복구
    // partner : 상대 조회 결과, blocked : 나와 상대의 차단 관계 (조회는 호출 측)
    public User validateDirectPartner(Optional<User> partner, boolean blocked) {
        User active = partner
                .filter(user -> user.getDeletedAt() == null && user.getStatus() != UserStatus.BANNED)
                .orElseThrow(() -> new CommonException(ChatErrorCode.CHAT_BLOCKED));
        if (blocked) {
            throw new CommonException(ChatErrorCode.CHAT_BLOCKED);
        }
        return active;
    }

    public Long getDirectPartnerId(Chatroom chatroom, User me) {
        String[] ids = chatroom.getDirectKey().split(":");
        Long first = Long.parseLong(ids[0]);
        Long second = Long.parseLong(ids[1]);
        return first.equals(me.getId()) ? second : first;
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
