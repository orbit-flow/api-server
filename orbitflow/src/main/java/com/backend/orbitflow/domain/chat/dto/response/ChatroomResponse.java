package com.backend.orbitflow.domain.chat.dto.response;

import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.enums.ChatroomType;
import com.backend.orbitflow.domain.user.entity.User;

// teamUuid : 팀 채팅방이면 팀 uuid (내부 id 비노출)
// 참여자는 개수만 포함하고 목록은 GET /api/chatrooms/{chatroomUuid}/members 로 페이지 조회
// counterpart* : 1:1 대화의 상대방 (상대가 나갔거나 다수 참여 대화면 null)
// lastMessage·unreadCount : 목록 조회 시에만 포함
public record ChatroomResponse(
        String uuid,
        ChatroomType type,
        String title,
        String teamUuid,
        long memberCount,
        String counterpartUuid,
        String counterpartName,
        String counterpartProfileImage,
        MessageResponse lastMessage,
        Long unreadCount
) {

    public static ChatroomResponse of(Chatroom chatroom, long memberCount, User counterpart, MessageResponse lastMessage, Long unreadCount) {
        return new ChatroomResponse(
                chatroom.getUuid(),
                chatroom.getType(),
                chatroom.getTitle(),
                chatroom.isTeamChatroom() ? chatroom.getTeam().getUuid() : null,
                memberCount,
                counterpart == null ? null : counterpart.getUuid(),
                counterpart == null ? null : counterpart.getName(),
                counterpart == null ? null : counterpart.getProfileImage(),
                lastMessage,
                unreadCount
        );
    }
}
