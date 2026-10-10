package com.backend.orbitflow.domain.chat.websocket;

import com.backend.orbitflow.domain.block.event.UserBlockedEvent;
import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.repository.ChatroomMemberRepository;
import com.backend.orbitflow.domain.chat.repository.ChatroomRepository;
import com.backend.orbitflow.domain.suspension.event.UserSuspendedEvent;
import com.backend.orbitflow.domain.user.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 권한 변경(채팅 도메인의 ChatSubscriptionRevokeEvent, 다른 도메인의 탈퇴·정지·차단 이벤트) 트랜잭션이 커밋된 뒤에만 구독 해제 (롤백된 변경으로 구독이 끊기지 않도록), 트랜잭션 밖에서 발행되어도 즉시 처리
@Component
@RequiredArgsConstructor
public class ChatSubscriptionRevokeListener {

    private static final int IN_CHUNK = 1000;

    private final ChatSubscriptionRegistry registry;
    private final ChatroomMemberRepository chatroomMemberRepository;
    private final ChatroomRepository chatroomRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void handle(ChatSubscriptionRevokeEvent event) {
        if (!event.recheck()) {
            registry.revoke(event.userUuid(), event.chatroomUuid());
            return;
        }
        // 현재 참여자가 아닌 채팅방의 구독만 해제
        Set<String> userUuids = event.userUuid() == null ? registry.subscribedUserUuids() : Set.of(event.userUuid());
        Map<String, Set<String>> subscribed = new HashMap<>();
        for (String userUuid : userUuids) {
            Set<String> chatroomUuids = registry.subscribedChatroomUuids(userUuid);
            if (!chatroomUuids.isEmpty()) {
                subscribed.put(userUuid, chatroomUuids);
            }
        }
        Map<String, Set<String>> joined = findJoined(subscribed);
        subscribed.forEach((userUuid, chatroomUuids) -> {
            Set<String> joinedChatroomUuids = joined.getOrDefault(userUuid, Set.of());
            chatroomUuids.stream()
                    .filter(chatroomUuid -> !joinedChatroomUuids.contains(chatroomUuid))
                    .forEach(chatroomUuid -> registry.revoke(userUuid, chatroomUuid));
        });
    }

    // 구독 쌍 수와 무관하게 참여 관계를 묶어서 조회 (IN 목록이 너무 길어지지 않도록 사용자·채팅방을 1000개 단위로 나눔, 보통 1회)
    private Map<String, Set<String>> findJoined(Map<String, Set<String>> subscribed) {
        Map<String, Set<String>> joined = new HashMap<>();
        List<String> userUuids = new ArrayList<>(subscribed.keySet());
        for (int from = 0; from < userUuids.size(); from += IN_CHUNK) {
            List<String> userChunk = userUuids.subList(from, Math.min(from + IN_CHUNK, userUuids.size()));
            List<String> chatroomUuids = userChunk.stream()
                    .flatMap(userUuid -> subscribed.get(userUuid).stream())
                    .distinct()
                    .toList();
            for (int i = 0; i < chatroomUuids.size(); i += IN_CHUNK) {
                chatroomMemberRepository.findMemberUuidsIn(chatroomUuids.subList(i, Math.min(i + IN_CHUNK, chatroomUuids.size())), userChunk)
                        .forEach(row -> joined.computeIfAbsent(row.getUserUuid(), key -> new HashSet<>()).add(row.getChatroomUuid()));
            }
        }
        return joined;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void handle(UserBlockedEvent event) {
        chatroomRepository.findByDirectKey(Chatroom.directKey(event.blockerId(), event.blockeeId()))
                .ifPresent(chatroom -> registry.revokeChatroom(chatroom.getUuid()));
    }

    // 탈퇴·정지 : 사용자의 모든 구독 해제
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(UserWithdrawnEvent event) {
        registry.revoke(event.userUuid(), null);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(UserSuspendedEvent event) {
        registry.revoke(event.userUuid(), null);
    }
}
