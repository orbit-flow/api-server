package com.backend.orbitflow.domain.chat.dto;

// 내 채팅방 목록 페이지 한 줄 : 채팅방 id와 참여 이후 마지막 메시지 id (메시지가 없으면 null)
public interface ChatroomPageRow {

    Long getChatroomId();
    Long getLastMessageId();
}
