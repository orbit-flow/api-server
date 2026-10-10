package com.backend.orbitflow.domain.chat.facade;

import com.backend.orbitflow.domain.chat.dto.request.ChatRequests;
import com.backend.orbitflow.domain.chat.dto.response.ChatroomResponse;
import com.backend.orbitflow.domain.chat.dto.response.MessageResponse;
import com.backend.orbitflow.domain.chat.service.ChatroomService;
import com.backend.orbitflow.domain.chat.service.MessageService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.backend.orbitflow.domain.chat.dto.response.ChatroomMemberResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;

@Component
@RequiredArgsConstructor
public class ChatFacade {

    private final ChatroomService chatroomService;
    private final MessageService messageService;
    private final UserService userService;
    private final TeamService teamService;

    @Transactional
    public ChatroomResponse openDirect(AuthUser authUser, ChatRequests.DirectChatroomRequest request) {
        return chatroomService.openDirect(me(authUser), userService.getByUuid(request.userUuid()));
    }

    @Transactional
    public ChatroomResponse createGroup(AuthUser authUser, ChatRequests.GroupChatroomRequest request) {
        return chatroomService.createGroup(me(authUser), request.title(), users(request.userUuids()));
    }

    @Transactional
    public ChatroomResponse createTeamGroup(AuthUser authUser, String teamUuid, ChatRequests.GroupChatroomRequest request) {
        return chatroomService.createTeamGroup(me(authUser), teamService.getActiveTeam(teamUuid), request.title(), users(request.userUuids()));
    }

    @Transactional
    public ChatroomResponse invite(AuthUser authUser, String chatroomUuid, ChatRequests.InviteRequest request) {
        return chatroomService.invite(me(authUser), chatroomUuid, users(request.userUuids()));
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
        return PageResponse.from(chatroomService.getMyChatrooms(me(authUser), page, size));
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
        return PageResponse.from(messageService.getMessages(me(authUser), chatroomUuid, beforeId, page, size));
    }

    @Transactional
    public MessageResponse send(AuthUser authUser, String chatroomUuid, ChatRequests.SendMessageRequest request) {
        return messageService.send(me(authUser), chatroomUuid, request.content(), request.clientMessageId());
    }

    @Transactional
    public void deleteMessage(AuthUser authUser, Long messageId, ChatRequests.DeleteMessageRequest request) {
        messageService.delete(me(authUser), messageId, request.scope());
    }

    private User me(AuthUser authUser) {
        return userService.getByUuid(authUser.getUuid());
    }

    private List<User> users(List<String> uuids) {
        return userService.getAllByUuids(uuids);
    }
}
