package com.backend.orbitflow.domain.chat.dto.response;

import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.enums.ChatroomType;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;

// teamUuid : 팀 채팅방이면 팀 uuid (내부 id 비노출)
// lastMessage·unreadCount : 목록 조회 시에만 포함
public record ChatroomResponse(
        String uuid,
        ChatroomType type,
        String title,
        String teamUuid,
        List<Member> members,
        MessageResponse lastMessage,
        Long unreadCount
) {

    public record Member(String uuid, String name, String profileImage) {

        public static Member from(User user) {
            return new Member(user.getUuid(), user.getName(), user.getProfileImage());
        }
    }

    public static ChatroomResponse of(Chatroom chatroom, List<User> members, MessageResponse lastMessage, Long unreadCount) {
        return new ChatroomResponse(
                chatroom.getUuid(),
                chatroom.getType(),
                chatroom.getTitle(),
                chatroom.isTeamChatroom() ? chatroom.getTeam().getUuid() : null,
                members.stream().map(Member::from).toList(),
                lastMessage,
                unreadCount
        );
    }
}
