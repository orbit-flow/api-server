package com.backend.orbitflow.domain.chat.dto;

// 채팅방별 집계 결과 (안 읽은 메시지 수)
public record ChatroomCount(
        Long chatroomId,
        Long count
) {
}
