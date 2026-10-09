package com.backend.orbitflow.domain.chat.dto.response;

// STOMP /sub/chatrooms/{uuid} 로 전달되는 실시간 이벤트
// MESSAGE : 새 메시지, UNSENT : 전송 취소 (message.unsent = true)
public record ChatEvent(
        Type type,
        MessageResponse message
) {

    public enum Type {
        MESSAGE, UNSENT
    }
}
