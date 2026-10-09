package com.backend.orbitflow.domain.chat.service;

import com.backend.orbitflow.domain.block.repository.BlockRepository;
import com.backend.orbitflow.domain.block.service.BlockService;
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
import com.backend.orbitflow.domain.user.repository.UserRepository;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageHideRepository messageHideRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final BlockService blockService;
    private final ChatroomService chatroomService;
    private final ChatPublisher chatPublisher;

    // 응답이 곧 전송 완료 확인 (클라이언트는 응답 전까지 전송 중 상태 표시)
    // 같은 clientMessageId로 재요청하면 새로 저장하지 않고 기존 메시지 반환
    // 실패한 전송은 클라이언트가 실패 상태로 유지하고, 사용자가 재전송을 요청할 때만 새 clientMessageId로 다시 전송
    public MessageResponse send(User me, String chatroomUuid, String content, String clientMessageId) {
        Chatroom chatroom = chatroomService.getChatroom(chatroomUuid);
        ChatroomMember member = chatroomService.getMember(chatroom, me);

        if (clientMessageId != null) {
            Optional<Message> existing = messageRepository.findBySenderAndClientMessageId(me, clientMessageId);
            if (existing.isPresent()) {
                return MessageResponse.of(existing.get(), chatroomUuid, false);
            }
        }
        if (chatroom.isDirect()) {
            prepareDirectPartner(chatroom, me);
        }

        Message message = messageRepository.save(Message.of(chatroom, me, content, clientMessageId));
        member.read();
        MessageResponse response = MessageResponse.of(message, chatroomUuid, false);
        chatPublisher.publishAfterCommit(chatroomUuid, new ChatEvent(ChatEvent.Type.MESSAGE, response));
        return response;
    }

    // 참여 이후 메시지만 최신순 (beforeId 미지정 시 가장 최근부터), 내가 차단한 사용자의 메시지는 blockedSender 표시
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(User me, String chatroomUuid, Long beforeId, int size) {
        Chatroom chatroom = chatroomService.getChatroom(chatroomUuid);
        ChatroomMember member = chatroomService.getMember(chatroom, me);
        Set<Long> blockeeIds = blockRepository.findBlockeeIds(me);
        return messageRepository.findVisibleMessages(
                        chatroom, me, member.joinedAt(),
                        beforeId == null ? Long.MAX_VALUE : beforeId,
                        PageRequest.of(0, Math.min(Math.max(size, 1), MAX_PAGE_SIZE))
                ).stream()
                .map(message -> MessageResponse.of(message, chatroomUuid, blockeeIds.contains(message.getSender().getId())))
                .toList();
    }

    // ME : 내 화면에서만 삭제 / ALL : 작성자가 모든 참여자 대상 전송 취소 (즉시 적용)
    public void delete(User me, Long messageId, MessageDeleteScope scope) {
        Message message = messageRepository.findWithAllById(messageId).orElseThrow(
                () -> new CommonException(ChatErrorCode.MESSAGE_NOT_FOUND)
        );
        Chatroom chatroom = message.getChatroom();
        ChatroomMember member = chatroomService.getMember(chatroom, me);
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
    // 상대가 대화에서 나갔으면 다시 참여시킴 (상대는 이 메시지부터 열람)
    private void prepareDirectPartner(Chatroom chatroom, User me) {
        Long partnerId = partnerId(chatroom, me);
        User partner = userRepository.findById(partnerId)
                .filter(user -> user.getDeletedAt() == null && user.getStatus() != UserStatus.BANNED)
                .orElseThrow(() -> new CommonException(ChatErrorCode.CHAT_BLOCKED));
        if (blockService.isBlocked(me, partner)) {
            throw new CommonException(ChatErrorCode.CHAT_BLOCKED);
        }
        chatroomService.ensureMember(chatroom, partner);
    }

    private Long partnerId(Chatroom chatroom, User me) {
        String[] ids = chatroom.getDirectKey().split(":");
        Long first = Long.parseLong(ids[0]);
        Long second = Long.parseLong(ids[1]);
        return first.equals(me.getId()) ? second : first;
    }
}
