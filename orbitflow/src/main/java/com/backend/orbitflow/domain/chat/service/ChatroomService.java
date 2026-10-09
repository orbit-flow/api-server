package com.backend.orbitflow.domain.chat.service;

import com.backend.orbitflow.domain.chat.dto.response.ChatroomResponse;
import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.entity.ChatroomMember;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;

public interface ChatroomService {

    ChatroomResponse openDirect(User me, User target);
    ChatroomResponse createGroup(User me, String title, List<User> users);
    ChatroomResponse createTeamGroup(User me, Team team, String title, List<User> users);
    ChatroomResponse invite(User me, String chatroomUuid, List<User> users);
    void leave(User me, String chatroomUuid);
    void read(User me, String chatroomUuid);
    List<ChatroomResponse> getMyChatrooms(User me);
    ChatroomResponse getChatroom(User me, String chatroomUuid);

    // 메시지 서비스에서 사용
    Chatroom getChatroom(String chatroomUuid);
    ChatroomMember getMember(Chatroom chatroom, User user);
    void ensureMember(Chatroom chatroom, User user);
}
