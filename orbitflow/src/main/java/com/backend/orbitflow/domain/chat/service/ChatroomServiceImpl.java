package com.backend.orbitflow.domain.chat.service;

import com.backend.orbitflow.domain.block.repository.BlockRepository;
import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.chat.dto.ChatroomCount;
import com.backend.orbitflow.domain.chat.dto.response.ChatroomResponse;
import com.backend.orbitflow.domain.chat.dto.response.MessageResponse;
import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.entity.ChatroomMember;
import com.backend.orbitflow.domain.chat.entity.Message;
import com.backend.orbitflow.domain.chat.error.ChatErrorCode;
import com.backend.orbitflow.domain.chat.repository.ChatroomMemberRepository;
import com.backend.orbitflow.domain.chat.repository.ChatroomRepository;
import com.backend.orbitflow.domain.chat.repository.MessageRepository;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatroomServiceImpl implements ChatroomService {

    private final ChatroomRepository chatroomRepository;
    private final ChatroomMemberRepository chatroomMemberRepository;
    private final MessageRepository messageRepository;
    private final BlockService blockService;
    private final BlockRepository blockRepository;
    private final FollowService followService;
    private final TeamAuthorityService teamAuthorityService;

    // 두 사용자 사이의 1:1 대화는 하나만 존재 (direct_key unique), 나갔던 대화면 다시 참여 (참여 이후 메시지만 열람)
    // TODO: 같은 두 사용자의 최초 생성 요청이 동시에 들어오면 한쪽은 unique 위반으로 실패 (재요청 시 기존 대화 반환)
    public ChatroomResponse openDirect(User me, User target) {
        validateInvitable(me, target);
        Chatroom chatroom = chatroomRepository.findByDirectKey(Chatroom.directKey(me.getId(), target.getId()))
                .orElseGet(() -> {
                    Chatroom created = chatroomRepository.save(Chatroom.direct(newUuid(), me.getId(), target.getId()));
                    chatroomMemberRepository.save(ChatroomMember.of(created, target));
                    return created;
                });
        ensureMember(chatroom, me);
        return toResponse(chatroom);
    }

    // 다수 참여 대화 : 나를 포함해 3명 이상
    public ChatroomResponse createGroup(User me, String title, List<User> users) {
        List<User> participants = distinctOthers(me, users);
        participants.forEach(user -> validateInvitable(me, user));
        return createGroupChatroom(me, null, title, participants);
    }

    // 팀 채팅방 : 팀 구성원만 참여 (팀 내부 대화이므로 팔로우 기반 초대 제한은 적용하지 않음)
    public ChatroomResponse createTeamGroup(User me, Team team, String title, List<User> users) {
        teamAuthorityService.getMember(team, me);
        List<User> participants = distinctOthers(me, users);
        participants.forEach(user -> validateTeamMember(team, user));
        return createGroupChatroom(me, team, title, participants);
    }

    // 1:1 대화에서 초대하면 기존 대화는 유지하고 기존 참여자와 초대 대상이 참여하는 새 다수 참여 대화 생성
    public ChatroomResponse invite(User me, String chatroomUuid, List<User> users) {
        Chatroom chatroom = getChatroom(chatroomUuid);
        getMember(chatroom, me);
        List<User> invitees = distinctUsers(users);

        if (chatroom.isDirect()) {
            List<User> participants = new ArrayList<>(memberUsers(chatroom));
            for (User invitee : invitees) {
                if (participants.stream().noneMatch(user -> user.getId().equals(invitee.getId()))) {
                    validateInvitable(me, invitee);
                    participants.add(invitee);
                }
            }
            participants.removeIf(user -> user.getId().equals(me.getId()));
            return createGroupChatroom(me, null, null, participants);
        }

        for (User invitee : invitees) {
            if (chatroomMemberRepository.findByChatroomAndUser(chatroom, invitee).isPresent()) {
                throw new CommonException(ChatErrorCode.ALREADY_CHAT_MEMBER);
            }
            if (chatroom.isTeamChatroom()) {
                validateTeamMember(chatroom.getTeam(), invitee);
            } else {
                validateInvitable(me, invitee);
            }
            chatroomMemberRepository.save(ChatroomMember.of(chatroom, invitee));
        }
        return toResponse(chatroom);
    }

    // 나간 즉시 대화와 과거 메시지 열람 불가, 다른 참여자의 메시지 기록은 유지
    public void leave(User me, String chatroomUuid) {
        chatroomMemberRepository.delete(getMember(getChatroom(chatroomUuid), me));
    }

    public void read(User me, String chatroomUuid) {
        getMember(getChatroom(chatroomUuid), me).read();
    }

    // 마지막 메시지 최신순, 마지막 메시지·안 읽은 수·참여자를 묶음 조회 (N+1 방지)
    @Transactional(readOnly = true)
    public List<ChatroomResponse> getMyChatrooms(User me) {
        List<Chatroom> chatrooms = chatroomMemberRepository.findAllWithChatroomByUser(me).stream()
                .map(ChatroomMember::getChatroom)
                .toList();
        if (chatrooms.isEmpty()) {
            return List.of();
        }
        Map<Long, List<User>> membersByRoom = chatroomMemberRepository.findAllWithUserByChatroomIn(chatrooms).stream()
                .collect(Collectors.groupingBy(member -> member.getChatroom().getId(),
                        Collectors.mapping(ChatroomMember::getUser, Collectors.toList())));
        Map<Long, Message> lastByRoom = messageRepository.findLastMessages(me).stream()
                .collect(Collectors.toMap(message -> message.getChatroom().getId(), message -> message));
        Map<Long, Long> unreadByRoom = chatroomMemberRepository.countUnread(me).stream()
                .collect(Collectors.toMap(ChatroomCount::chatroomId, ChatroomCount::count));
        Set<Long> blockeeIds = blockRepository.findBlockeeIds(me);

        Map<Chatroom, Long> sortKey = new LinkedHashMap<>();
        chatrooms.forEach(chatroom -> sortKey.put(chatroom,
                lastByRoom.containsKey(chatroom.getId()) ? lastByRoom.get(chatroom.getId()).getId() : 0L));

        return chatrooms.stream()
                .sorted(Comparator.comparing((Chatroom chatroom) -> sortKey.get(chatroom)).reversed())
                .map(chatroom -> {
                    Message last = lastByRoom.get(chatroom.getId());
                    MessageResponse lastResponse = last == null ? null
                            : MessageResponse.of(last, chatroom.getUuid(), blockeeIds.contains(last.getSender().getId()));
                    return ChatroomResponse.of(chatroom, membersByRoom.getOrDefault(chatroom.getId(), List.of()),
                            lastResponse, unreadByRoom.getOrDefault(chatroom.getId(), 0L));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatroomResponse getChatroom(User me, String chatroomUuid) {
        Chatroom chatroom = getChatroom(chatroomUuid);
        getMember(chatroom, me);
        return toResponse(chatroom);
    }

    @Transactional(readOnly = true)
    public Chatroom getChatroom(String chatroomUuid) {
        return chatroomRepository.findByUuid(chatroomUuid).orElseThrow(
                () -> new CommonException(ChatErrorCode.CHATROOM_NOT_FOUND)
        );
    }

    // 참여자가 아니면 대화 존재 여부를 노출하지 않도록 NOT_FOUND 처리
    @Transactional(readOnly = true)
    public ChatroomMember getMember(Chatroom chatroom, User user) {
        return chatroomMemberRepository.findByChatroomAndUser(chatroom, user).orElseThrow(
                () -> new CommonException(ChatErrorCode.CHATROOM_NOT_FOUND)
        );
    }

    public void ensureMember(Chatroom chatroom, User user) {
        if (chatroomMemberRepository.findByChatroomAndUser(chatroom, user).isEmpty()) {
            chatroomMemberRepository.save(ChatroomMember.of(chatroom, user));
        }
    }

    private ChatroomResponse createGroupChatroom(User me, Team team, String title, List<User> others) {
        if (others.size() < 2) {
            throw new CommonException(ChatErrorCode.GROUP_MIN_MEMBERS);
        }
        Chatroom chatroom = chatroomRepository.save(Chatroom.group(newUuid(), team, title));
        chatroomMemberRepository.save(ChatroomMember.of(chatroom, me));
        others.forEach(user -> chatroomMemberRepository.save(ChatroomMember.of(chatroom, user)));
        return toResponse(chatroom);
    }

    // 차단 관계 불가, 상대가 팔로우하지 않는 사용자의 초대를 막아 두었으면 상대가 나를 팔로우 중이어야 함
    private void validateInvitable(User inviter, User invitee) {
        if (inviter.getId().equals(invitee.getId())) {
            throw new CommonException(ChatErrorCode.SELF_CHAT);
        }
        if (blockService.isBlocked(inviter, invitee)) {
            throw new CommonException(ChatErrorCode.CHAT_BLOCKED);
        }
        if (!invitee.isAllowNonFollowChatInvite()
                && followService.getFollowState(invitee, inviter) != FollowState.ACCEPTED) {
            throw new CommonException(ChatErrorCode.CHAT_INVITE_NOT_ALLOWED);
        }
    }

    private void validateTeamMember(Team team, User user) {
        if (teamAuthorityService.findMember(team, user).isEmpty()) {
            throw new CommonException(TeamErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private List<User> distinctOthers(User me, List<User> users) {
        return distinctUsers(users).stream()
                .filter(user -> !user.getId().equals(me.getId()))
                .toList();
    }

    private List<User> distinctUsers(List<User> users) {
        Map<Long, User> byId = new LinkedHashMap<>();
        users.forEach(user -> byId.putIfAbsent(user.getId(), user));
        return new ArrayList<>(byId.values());
    }

    private List<User> memberUsers(Chatroom chatroom) {
        return chatroomMemberRepository.findAllWithUserByChatroom(chatroom).stream()
                .map(ChatroomMember::getUser)
                .toList();
    }

    private ChatroomResponse toResponse(Chatroom chatroom) {
        return ChatroomResponse.of(chatroom, memberUsers(chatroom), null, null);
    }

    private String newUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
