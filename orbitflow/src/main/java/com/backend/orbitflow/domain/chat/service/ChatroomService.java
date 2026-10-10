package com.backend.orbitflow.domain.chat.service;

import com.backend.orbitflow.domain.chat.dto.response.ChatroomResponse;
import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.entity.ChatroomMember;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;
import com.backend.orbitflow.domain.chat.dto.response.ChatroomMemberResponse;
import org.springframework.data.domain.Page;

public interface ChatroomService {

    ChatroomResponse openDirect(User me, User target);
    ChatroomResponse createGroup(User me, String title, List<User> users);
    ChatroomResponse createTeamGroup(User me, Team team, String title, List<User> users);
    ChatroomResponse invite(User me, String chatroomUuid, List<User> users);
    void leave(User me, String chatroomUuid);
    void read(User me, String chatroomUuid);
    Page<ChatroomResponse> getMyChatrooms(User me, int page, int size);
    Page<ChatroomMemberResponse> getMembers(User me, String chatroomUuid, int page, int size);
    ChatroomResponse getChatroom(User me, String chatroomUuid);

    // 메시지 서비스에서 사용
    Chatroom getChatroom(String chatroomUuid);
    ChatroomMember getMember(Chatroom chatroom, User user);
    void ensureMember(Chatroom chatroom, User user);

    // 팀 서비스에서 사용 : 팀 구성원 이탈·팀 삭제 시 팀 채팅방에서 제외
    void leaveTeamChatrooms(Team team, User user);
    void removeTeamChatroomMembers(Team team);
}
