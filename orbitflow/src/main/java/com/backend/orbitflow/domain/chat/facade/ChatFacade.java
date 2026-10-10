package com.backend.orbitflow.domain.chat.facade;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.chat.dto.request.ChatRequests;
import com.backend.orbitflow.domain.chat.dto.response.ChatroomResponse;
import com.backend.orbitflow.domain.chat.dto.response.MessageResponse;
import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.entity.ChatroomMember;
import com.backend.orbitflow.domain.chat.entity.Message;
import com.backend.orbitflow.domain.chat.service.ChatroomService;
import com.backend.orbitflow.domain.chat.service.MessageService;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.backend.orbitflow.domain.chat.dto.response.ChatroomMemberResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;

@Component
@RequiredArgsConstructor
public class ChatFacade {

    private final ChatroomService chatroomService;
    private final MessageService messageService;
    private final UserService userService;
    private final TeamService teamService;
    private final TeamAuthorityService teamAuthorityService;
    private final BlockService blockService;
    private final FollowService followService;

    @Transactional
    public ChatroomResponse openDirect(AuthUser authUser, ChatRequests.DirectChatroomRequest request) {
        User me = me(authUser);
        User target = userService.getByUuid(request.userUuid());
        List<Long> targetIds = List.of(target.getId());
        return chatroomService.openDirect(me, target, blockedAmong(me, targetIds), followerIdsAmong(me, targetIds));
    }

    @Transactional
    public ChatroomResponse createGroup(AuthUser authUser, ChatRequests.GroupChatroomRequest request) {
        User me = me(authUser);
        List<User> users = users(request.userUuids());
        List<User> participants = chatroomService.findOtherUsers(me, users);
        List<Long> participantIds = ids(participants);
        List<Long> blockIds = chatroomService.getGroupBlockIds(me, participants);
        return chatroomService.createGroup(me, request.title(), users,
                blockedAmong(me, participantIds), followerIdsAmong(me, participantIds), existsBlockAmong(blockIds, blockIds));
    }

    @Transactional
    public ChatroomResponse createTeamGroup(AuthUser authUser, String teamUuid, ChatRequests.GroupChatroomRequest request) {
        User me = me(authUser);
        Team team = teamService.getActiveTeam(teamUuid);
        List<User> users = users(request.userUuids());
        // 팀 구성원만 팀 채팅방 생성 가능 (구성원이 아니면 예외)
        teamAuthorityService.getMember(team, me);
        List<User> participants = chatroomService.findOtherUsers(me, users);
        List<Long> blockIds = chatroomService.getGroupBlockIds(me, participants);
        return chatroomService.createTeamGroup(me, team, request.title(), users,
                teamMemberIds(team, participants), existsBlockAmong(blockIds, blockIds));
    }

    @Transactional
    public ChatroomResponse invite(AuthUser authUser, String chatroomUuid, ChatRequests.InviteRequest request) {
        User me = me(authUser);
        List<User> users = users(request.userUuids());
        // 채팅방 확인 후 초대 대상 전체 기준으로 미리 조회 (서비스가 기존 참여자 제외 등 실제 검증 대상을 걸러 판정)
        Chatroom chatroom = chatroomService.getChatroom(chatroomUuid);
        chatroomService.getMember(chatroom, me);
        List<User> invitees = chatroomService.findDistinctUsers(users);
        List<Long> inviteeIds = ids(invitees);
        Set<Long> teamMemberIds = chatroom.isTeamChatroom() ? teamMemberIds(chatroom.getTeam(), invitees) : Set.of();
        boolean blockAmong = existsBlockAmong(chatroomService.getInviteBlockTargetIds(chatroom, invitees),
                chatroomService.getInviteBlockAllIds(chatroom, invitees));
        return chatroomService.invite(me, chatroomUuid, users, blockedAmong(me, inviteeIds), followerIdsAmong(me, inviteeIds),
                teamMemberIds, blockAmong);
    }

    @Transactional
    public void leave(AuthUser authUser, String chatroomUuid) {
        chatroomService.leave(me(authUser), chatroomUuid);
    }

    @Transactional
    public void read(AuthUser authUser, String chatroomUuid) {
        chatroomService.read(me(authUser), chatroomUuid);
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatroomResponse> getMyChatrooms(AuthUser authUser, int page, int size) {
        User me = me(authUser);
        return PageResponse.from(chatroomService.getMyChatrooms(me, page, size, blockService.findBlockeeIds(me)));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatroomMemberResponse> getMembers(AuthUser authUser, String chatroomUuid, int page, int size) {
        return PageResponse.from(chatroomService.getMembers(me(authUser), chatroomUuid, page, size));
    }

    @Transactional(readOnly = true)
    public ChatroomResponse getChatroom(AuthUser authUser, String chatroomUuid) {
        return chatroomService.getChatroom(me(authUser), chatroomUuid);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getMessages(AuthUser authUser, String chatroomUuid, Long beforeId, int page, int size) {
        User me = me(authUser);
        Chatroom chatroom = chatroomService.getChatroom(chatroomUuid);
        ChatroomMember member = chatroomService.getMember(chatroom, me);
        Set<Long> blockeeIds = blockService.findBlockeeIds(me);
        return PageResponse.from(messageService.getMessages(me, chatroom, member, blockeeIds, beforeId, page, size));
    }

    @Transactional
    public MessageResponse send(AuthUser authUser, String chatroomUuid, ChatRequests.SendMessageRequest request) {
        User me = me(authUser);
        Chatroom chatroom = chatroomService.getChatroom(chatroomUuid);
        ChatroomMember member = chatroomService.getMember(chatroom, me);
        // 같은 clientMessageId로 재요청이면 상대 상태와 무관하게 기존 메시지 반환
        Optional<MessageResponse> sent = messageService.findSent(me, chatroom, request.clientMessageId());
        if (sent.isPresent()) {
            return sent.get();
        }
        if (chatroom.isDirect()) {
            Optional<User> partner = userService.findById(messageService.getDirectPartnerId(chatroom, me));
            boolean blocked = partner.isPresent() && blockService.isBlocked(me, partner.get());
            chatroomService.ensureMember(chatroom, messageService.validateDirectPartner(partner, blocked));
        }
        return messageService.send(me, chatroom, member, request.content(), request.clientMessageId());
    }

    @Transactional
    public void deleteMessage(AuthUser authUser, Long messageId, ChatRequests.DeleteMessageRequest request) {
        User me = me(authUser);
        Message message = messageService.getMessage(messageId);
        ChatroomMember member = chatroomService.getMember(message.getChatroom(), me);
        messageService.delete(me, message, member, request.scope());
    }

    // 초대자(me) 기준 차단·팔로우·팀 소속 묶음 조회 (대상이 없으면 조회하지 않음), 판정은 ChatroomService
    private Set<Long> blockedAmong(User me, List<Long> userIds) {
        return userIds.isEmpty() ? Set.of() : blockService.findBlockedUserIdsAmong(me, userIds);
    }

    private Set<Long> followerIdsAmong(User me, List<Long> userIds) {
        return userIds.isEmpty() ? Set.of() : followService.findFollowerIdsAmong(me, userIds);
    }

    private Set<Long> teamMemberIds(Team team, List<User> users) {
        return users.isEmpty() ? Set.of() : teamAuthorityService.findMemberUserIds(team, users);
    }

    private boolean existsBlockAmong(List<Long> targetIds, List<Long> allIds) {
        return !targetIds.isEmpty() && blockService.existsBlockAmong(targetIds, allIds);
    }

    private List<Long> ids(List<User> users) {
        return users.stream().map(User::getId).toList();
    }

    private User me(AuthUser authUser) {
        return userService.getActiveByUuid(authUser.getUuid());
    }

    private List<User> users(List<String> uuids) {
        return userService.getAllByUuids(uuids);
    }
}
