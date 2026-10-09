package com.backend.orbitflow.domain.chat.dto.response;

import com.backend.orbitflow.domain.chat.entity.Message;
import com.backend.orbitflow.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// content : 전송 취소된 메시지는 null
// blockedSender : 요청자가 차단한 사용자의 메시지 (FE에서 "차단한 사용자의 메시지"로 가리고, 보기 선택 시 content 표시)
// 실시간 전송(STOMP) 페이로드는 수신자 공통이므로 blockedSender = false, FE가 자신의 차단 목록으로 판단
public record MessageResponse(
        Long id,
        String chatroomUuid,
        Sender sender,
        String content,
        boolean unsent,
        boolean blockedSender,
        String clientMessageId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public record Sender(String uuid, String name, String profileImage) {
    }

    public static MessageResponse of(Message message, String chatroomUuid, boolean blockedSender) {
        User sender = message.getSender();
        return new MessageResponse(
                message.getId(),
                chatroomUuid,
                new Sender(sender.getUuid(), sender.getName(), sender.getProfileImage()),
                message.isUnsent() ? null : message.getContent(),
                message.isUnsent(),
                blockedSender,
                message.getClientMessageId(),
                message.getCreatedAt()
        );
    }
}
