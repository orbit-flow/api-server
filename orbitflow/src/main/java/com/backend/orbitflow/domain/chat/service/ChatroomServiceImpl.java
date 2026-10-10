package com.backend.orbitflow.domain.chat.service;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.chat.dto.ChatroomCount;
import com.backend.orbitflow.domain.chat.dto.ChatroomPageRow;
import com.backend.orbitflow.domain.chat.dto.response.ChatroomMemberResponse;
import com.backend.orbitflow.domain.chat.dto.response.ChatroomResponse;
import com.backend.orbitflow.domain.chat.dto.response.MessageResponse;
import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.entity.ChatroomMember;
import com.backend.orbitflow.domain.chat.entity.Message;
import com.backend.orbitflow.domain.chat.error.ChatErrorCode;
import com.backend.orbitflow.domain.chat.repository.ChatroomMemberRepository;
import com.backend.orbitflow.domain.chat.repository.ChatroomRepository;
import com.backend.orbitflow.domain.chat.repository.MessageRepository;
import com.backend.orbitflow.domain.chat.websocket.ChatSubscriptionRevokeEvent;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.backend.orbitflow.global.util.JdbcBulkInserter;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatroomServiceImpl implements ChatroomService {

    private final ChatroomRepository chatroomRepository;
    private final ChatroomMemberRepository chatroomMemberRepository;
    private final JdbcBulkInserter jdbcBulkInserter;
    private final MessageRepository messageRepository;
    private final BlockService blockService;
    private final FollowService followService;
    private final TeamAuthorityService teamAuthorityService;
    private final ApplicationEventPublisher eventPublisher;

    // 두 사용자 사이의 1:1 대화는 하나만 존재 (direct_key unique), 나갔던 대화면 다시 참여 (참여 이후 메시지만 열람)
    // TODO: 같은 두 사용자의 최초 생성 요청이 동시에 들어오면 한쪽은 unique 위반으로 실패 (재요청 시 기존 대화 반환)
    public ChatroomResponse openDirect(User me, User target) {
        validateInvitable(me, List.of(target));
        Chatroom chatroom = chatroomRepository.findByDirectKey(Chatroom.directKey(me.getId(), target.getId()))
                .orElseGet(() -> {
                    Chatroom created = chatroomRepository.save(Chatroom.direct(newUuid(), me.getId(), target.getId()));
                    chatroomMemberRepository.save(ChatroomMember.of(created, target));
                    return created;
                });
        ensureMember(chatroom, me);
        return toResponse(chatroom, me);
    }

    // 다수 참여 대화 : 나를 포함해 3명 이상
    public ChatroomResponse createGroup(User me, String title, List<User> users) {
        List<User> participants = distinctOthers(me, users);
        validateInvitable(me, participants);
        validateNoBlockAmong(withMe(me, participants), withMe(me, participants));
        return createGroupChatroom(me, null, title, participants);
    }

    // 팀 채팅방 : 팀 구성원만 참여 (팀 내부 대화이므로 팔로우 기반 초대 제한은 적용하지 않음)
    public ChatroomResponse createTeamGroup(User me, Team team, String title, List<User> users) {
        teamAuthorityService.getMember(team, me);
        List<User> participants = distinctOthers(me, users);
        validateTeamMembers(team, participants);
        validateNoBlockAmong(withMe(me, participants), withMe(me, participants));
        return createGroupChatroom(me, team, title, participants);
    }

    // 1:1 대화에서 초대하면 기존 대화는 유지하고 기존 참여자와 초대 대상이 참여하는 새 다수 참여 대화 생성
    // 초대 대상 검증(차단·팔로우·팀 소속·기존 참여)은 대상 수와 무관하게 묶음 조회
    public ChatroomResponse invite(User me, String chatroomUuid, List<User> users) {
        Chatroom chatroom = getChatroom(chatroomUuid);
        getMember(chatroom, me);
        List<User> invitees = distinctUsers(users);

        if (chatroom.isDirect()) {
            List<User> participants = new ArrayList<>(memberUsers(chatroom));
            List<User> newcomers = invitees.stream()
                    .filter(invitee -> participants.stream().noneMatch(user -> user.getId().equals(invitee.getId())))
                    .toList();
            validateInvitable(me, newcomers);
            participants.addAll(newcomers);
            // 새 대화의 최종 참여자(기존 상대 포함) 사이에 차단 관계가 하나라도 있으면 불가
            validateNoBlockAmong(ids(participants), ids(participants));
            participants.removeIf(user -> user.getId().equals(me.getId()));
            return createGroupChatroom(me, null, null, participants);
        }

        if (!chatroomMemberRepository.findMemberUserIdsAmong(chatroom, ids(invitees)).isEmpty()) {
            throw new CommonException(ChatErrorCode.ALREADY_CHAT_MEMBER);
        }
        if (chatroom.isTeamChatroom()) {
            validateTeamMembers(chatroom.getTeam(), invitees);
        } else {
            validateInvitable(me, invitees);
        }
        // 초대 대상과 기존 참여자·초대 대상끼리 차단 관계가 있으면 불가 (기존 참여자끼리의 관계는 검증하지 않음)
        List<Long> allIds = new ArrayList<>(chatroomMemberRepository.findMemberUserIds(chatroom));
        allIds.addAll(ids(invitees));
        validateNoBlockAmong(ids(invitees), allIds);
        insertMembers(chatroom, ids(invitees));
        return toResponse(chatroom, me);
    }

    // 나간 즉시 대화와 과거 메시지 열람 불가, 다른 참여자의 메시지 기록은 유지 (실시간 구독도 커밋 후 해제)
    public void leave(User me, String chatroomUuid) {
        chatroomMemberRepository.delete(getMember(getChatroom(chatroomUuid), me));
        eventPublisher.publishEvent(ChatSubscriptionRevokeEvent.chatroom(me.getUuid(), chatroomUuid));
    }

    public void read(User me, String chatroomUuid) {
        getMember(getChatroom(chatroomUuid), me).read();
    }

    // 마지막 메시지 최신순 페이지 : 페이지에 든 채팅방에 대해서만 부가 정보를 묶음 조회 (페이지 크기와 무관하게 쿼리 8회)
    // 목록 + 개수 → 채팅방 → 마지막 메시지 → 안 읽은 수 → 참여자 수 → 1:1 상대 → 내 차단 목록
    @Transactional(readOnly = true)
    public Page<ChatroomResponse> getMyChatrooms(User me, int page, int size) {
        Pageable pageable = toPageable(page, size);
        Page<ChatroomPageRow> rows = chatroomMemberRepository.findMyChatroomPage(me.getId(), pageable);
        if (rows.isEmpty()) {
            return rows.map(row -> null);
        }
        List<Long> chatroomIds = rows.getContent().stream().map(ChatroomPageRow::getChatroomId).toList();
        List<Long> lastMessageIds = rows.getContent().stream().map(ChatroomPageRow::getLastMessageId).filter(Objects::nonNull).toList();

        Map<Long, Chatroom> chatrooms = chatroomRepository.findAllWithTeamByIdIn(chatroomIds).stream()
                .collect(Collectors.toMap(Chatroom::getId, Function.identity()));
        Map<Long, Message> lastMessages = lastMessageIds.isEmpty() ? Map.of()
                : messageRepository.findAllWithSenderByIdIn(lastMessageIds).stream()
                .collect(Collectors.toMap(Message::getId, Function.identity()));
        Map<Long, Long> unread = toCountMap(chatroomMemberRepository.countUnreadIn(me, chatroomIds));
        Map<Long, Long> memberCounts = toCountMap(chatroomMemberRepository.countMembersIn(chatroomIds));
        Map<Long, User> counterparts = counterparts(chatroomIds, me);
        Set<Long> blockeeIds = blockService.findBlockeeIds(me);

        return rows.map(row -> {
            Chatroom chatroom = chatrooms.get(row.getChatroomId());
            Message last = row.getLastMessageId() == null ? null : lastMessages.get(row.getLastMessageId());
            MessageResponse lastResponse = last == null ? null
                    : MessageResponse.of(last, chatroom.getUuid(), blockeeIds.contains(last.getSender().getId()));
            return ChatroomResponse.of(chatroom, memberCounts.getOrDefault(chatroom.getId(), 0L),
                    counterparts.get(chatroom.getId()), lastResponse, unread.getOrDefault(chatroom.getId(), 0L));
        });
    }

    @Transactional(readOnly = true)
    public ChatroomResponse getChatroom(User me, String chatroomUuid) {
        Chatroom chatroom = getChatroom(chatroomUuid);
        getMember(chatroom, me);
        return toResponse(chatroom, me);
    }

    // 참여자만 조회 가능
    @Transactional(readOnly = true)
    public Page<ChatroomMemberResponse> getMembers(User me, String chatroomUuid, int page, int size) {
        Pageable pageable = toPageable(page, size);
        Chatroom chatroom = getChatroom(chatroomUuid);
        getMember(chatroom, me);
        return chatroomMemberRepository.findMemberPage(chatroom, pageable);
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

    // 팀 탈퇴·추방 시 해당 사용자를 팀 채팅방에서 제외하고 실시간 구독 해제 (커밋 후 참여자가 아닌 채팅방만)
    public void leaveTeamChatrooms(Team team, User user) {
        chatroomMemberRepository.deleteAllByTeamAndUser(team, user);
        eventPublisher.publishEvent(ChatSubscriptionRevokeEvent.recheck(user.getUuid()));
    }

    // 팀 삭제 시 팀 채팅방 참여자 전원 제외, 모든 구성원의 팀 채팅방 실시간 구독 해제 (커밋 후 참여자가 아닌 채팅방만)
    public void removeTeamChatroomMembers(Team team) {
        chatroomMemberRepository.deleteAllByTeam(team);
        eventPublisher.publishEvent(ChatSubscriptionRevokeEvent.recheckAll());
    }

    // 참여자는 JDBC 배치 1회로 저장 (참여자 수만큼 INSERT하지 않음)
    private ChatroomResponse createGroupChatroom(User me, Team team, String title, List<User> others) {
        if (others.size() < 2) {
            throw new CommonException(ChatErrorCode.GROUP_MIN_MEMBERS);
        }
        Chatroom chatroom = chatroomRepository.save(Chatroom.group(newUuid(), team, title));
        List<Long> memberIds = new ArrayList<>();
        memberIds.add(me.getId());
        memberIds.addAll(ids(others));
        insertMembers(chatroom, memberIds);
        return toResponse(chatroom, me);
    }

    // 차단 관계 불가, 상대가 팔로우하지 않는 사용자의 초대를 막아 두었으면 상대가 나를 팔로우 중이어야 함
    // 대상 수와 무관하게 쿼리 최대 2회 (차단 관계, 팔로우 관계)
    private void validateInvitable(User inviter, List<User> invitees) {
        if (invitees.isEmpty()) {
            return;
        }
        if (invitees.stream().anyMatch(invitee -> invitee.getId().equals(inviter.getId()))) {
            throw new CommonException(ChatErrorCode.SELF_CHAT);
        }
        if (!blockService.findBlockedUserIdsAmong(inviter, ids(invitees)).isEmpty()) {
            throw new CommonException(ChatErrorCode.CHAT_BLOCKED);
        }
        List<Long> followRequired = invitees.stream()
                .filter(invitee -> !invitee.isAllowNonFollowChatInvite())
                .map(User::getId)
                .toList();
        if (!followRequired.isEmpty()
                && !followService.findFollowerIdsAmong(inviter, followRequired).containsAll(followRequired)) {
            throw new CommonException(ChatErrorCode.CHAT_INVITE_NOT_ALLOWED);
        }
    }

    // targetIds가 allIds(targetIds 포함) 중 누구와도 차단 관계(어느 방향이든)가 아니어야 함 (쿼리 1회)
    private void validateNoBlockAmong(List<Long> targetIds, List<Long> allIds) {
        if (!targetIds.isEmpty() && blockService.existsBlockAmong(targetIds, allIds)) {
            throw new CommonException(ChatErrorCode.CHAT_BLOCKED);
        }
    }

    private List<Long> withMe(User me, List<User> others) {
        List<Long> userIds = new ArrayList<>(ids(others));
        userIds.add(me.getId());
        return userIds;
    }

    private void validateTeamMembers(Team team, List<User> users) {
        if (!users.isEmpty() && teamAuthorityService.findMemberUserIds(team, users).size() != users.size()) {
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

    private List<Long> ids(List<User> users) {
        return users.stream().map(User::getId).toList();
    }

    // 1:1 대화의 기존 참여자 (최대 2명, 다수 참여 대화로 옮길 때만 사용)
    private List<User> memberUsers(Chatroom chatroom) {
        return chatroomMemberRepository.findAllWithUserByChatroom(chatroom).stream()
                .map(ChatroomMember::getUser)
                .toList();
    }

    // 참여자 일괄 저장 (INSERT 1회)
    private void insertMembers(Chatroom chatroom, List<Long> userIds) {
        jdbcBulkInserter.insert("chatroom_members", List.of("chatroom_id", "user_id"),
                userIds.stream().map(userId -> new Object[]{chatroom.getId(), userId}).toList());
    }

    private Map<Long, User> counterparts(List<Long> chatroomIds, User me) {
        return chatroomMemberRepository.findDirectCounterparts(chatroomIds, me).stream()
                .collect(Collectors.toMap(member -> member.getChatroom().getId(), ChatroomMember::getUser, (a, b) -> a));
    }

    private Map<Long, Long> toCountMap(List<ChatroomCount> counts) {
        return counts.stream().collect(Collectors.toMap(ChatroomCount::chatroomId, ChatroomCount::count));
    }

    // 단건 응답 : 참여자 수 + (1:1이면) 상대방 (쿼리 2회)
    private ChatroomResponse toResponse(Chatroom chatroom, User me) {
        User counterpart = chatroom.isDirect() ? counterparts(List.of(chatroom.getId()), me).get(chatroom.getId()) : null;
        return ChatroomResponse.of(chatroom, chatroomMemberRepository.countByChatroom(chatroom), counterpart, null, null);
    }

    private String newUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
