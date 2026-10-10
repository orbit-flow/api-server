package com.backend.orbitflow.domain.chat.service;

import com.backend.orbitflow.domain.chat.dto.response.ChatroomResponse;
import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.entity.ChatroomMember;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;
import java.util.Set;
import com.backend.orbitflow.domain.chat.dto.response.ChatroomMemberResponse;
import org.springframework.data.domain.Page;

// 차단·팔로우·팀 소속 조회는 ChatFacade가 같은 트랜잭션에서 미리 수행해 id 집합·boolean으로 전달, 판정과 예외는 서비스에서
//  blockedIds : 초대자와 차단 관계인 id / followerIds : 초대자를 팔로우 중인 id / teamMemberIds : 팀 구성원인 id / blockAmong : 참여자 사이 차단 관계 존재 여부
public interface ChatroomService {

    ChatroomResponse openDirect(User me, User target, Set<Long> blockedIds, Set<Long> followerIds);
    ChatroomResponse createGroup(User me, String title, List<User> users,
                                 Set<Long> blockedIds, Set<Long> followerIds, boolean blockAmong);
    ChatroomResponse createTeamGroup(User me, Team team, String title, List<User> users,
                                     Set<Long> teamMemberIds, boolean blockAmong);
    ChatroomResponse invite(User me, String chatroomUuid, List<User> users,
                            Set<Long> blockedIds, Set<Long> followerIds,
                            Set<Long> teamMemberIds, boolean blockAmong);
    void leave(User me, String chatroomUuid);
    void read(User me, String chatroomUuid);
    Page<ChatroomResponse> getMyChatrooms(User me, int page, int size, Set<Long> blockeeIds);
    Page<ChatroomMemberResponse> getMembers(User me, String chatroomUuid, int page, int size);
    ChatroomResponse getChatroom(User me, String chatroomUuid);

    // 파사드의 차단·팀 조회 대상 결정용 (검증과 같은 기준의 사용자·id)
    List<User> findOtherUsers(User me, List<User> users);
    List<User> findDistinctUsers(List<User> users);
    List<Long> getGroupBlockIds(User me, List<User> participants);
    List<Long> getInviteBlockTargetIds(Chatroom chatroom, List<User> invitees);
    List<Long> getInviteBlockAllIds(Chatroom chatroom, List<User> invitees);

    // 메시지 서비스에서 사용
    Chatroom getChatroom(String chatroomUuid);
    ChatroomMember getMember(Chatroom chatroom, User user);
    void ensureMember(Chatroom chatroom, User user);

    // 팀 서비스에서 사용 : 팀 구성원 이탈·팀 삭제 시 팀 채팅방에서 제외
    void leaveTeamChatrooms(Team team, User user);
    void removeTeamChatroomMembers(Team team);
}
