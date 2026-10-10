package com.backend.orbitflow.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// 채팅방 참여자 한 줄 (ChatroomMemberRepository에서 바로 생성), uuid : 사용자 uuid, joinedAt : 참여 시각
public record ChatroomMemberResponse(
        String uuid,
        String name,
        String profileImage,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime joinedAt
) {
}
