package com.backend.orbitflow.domain.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

// STOMP 세션·구독 현황 (사용자 uuid, access token 만료 시각, 세션별 구독 -> 채팅방 uuid)
// 권한을 잃은 구독은 SimpleBroker로 UNSUBSCRIBE 메시지를 보내 해제 (소켓은 유지, 이후 해당 채팅방 메시지만 받지 못함)
// TODO: 다중 인스턴스 배포 시 인스턴스 간 전파 필요 (현재 SimpleBroker·레지스트리는 인스턴스 메모리)
@Slf4j
@Component
public class ChatSubscriptionRegistry {

    private record SessionInfo(String userUuid, long expiresAtMillis) {
    }

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    // sessionId -> (subscriptionId -> chatroomUuid)
    private final Map<String, Map<String, String>> subscriptions = new ConcurrentHashMap<>();
    // clientInboundChannel 생성이 StompAuthInterceptor(→ 이 레지스트리)를 필요로 하므로 순환 방지를 위해 지연 주입
    private final MessageChannel clientInboundChannel;

    public ChatSubscriptionRegistry(@Lazy @Qualifier("clientInboundChannel") MessageChannel clientInboundChannel) {
        this.clientInboundChannel = clientInboundChannel;
    }

    public void registerSession(String sessionId, String userUuid, long expiresAtMillis) {
        sessions.put(sessionId, new SessionInfo(userUuid, expiresAtMillis));
    }

    public boolean isExpired(String sessionId) {
        SessionInfo info = sessions.get(sessionId);
        return info == null || info.expiresAtMillis() <= System.currentTimeMillis();
    }

    public void addSubscription(String sessionId, String subscriptionId, String chatroomUuid) {
        subscriptions.computeIfAbsent(sessionId, key -> new ConcurrentHashMap<>()).put(subscriptionId, chatroomUuid);
    }

    public void removeSubscription(String sessionId, String subscriptionId) {
        Map<String, String> bySession = subscriptions.get(sessionId);
        if (bySession != null) {
            bySession.remove(subscriptionId);
        }
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        subscriptions.remove(sessionId);
    }

    // 현재 구독 중인 사용자 uuid
    public Set<String> subscribedUserUuids() {
        Set<String> userUuids = new HashSet<>();
        subscriptions.keySet().forEach(sessionId -> {
            SessionInfo info = sessions.get(sessionId);
            if (info != null) {
                userUuids.add(info.userUuid());
            }
        });
        return userUuids;
    }

    // 사용자가 구독 중인 채팅방 uuid
    public Set<String> subscribedChatroomUuids(String userUuid) {
        Set<String> chatroomUuids = new HashSet<>();
        subscriptions.forEach((sessionId, bySession) -> {
            SessionInfo info = sessions.get(sessionId);
            if (info != null && info.userUuid().equals(userUuid)) {
                chatroomUuids.addAll(bySession.values());
            }
        });
        return chatroomUuids;
    }

    // 사용자의 구독 해제 : chatroomUuid가 null이면 해당 사용자의 모든 구독
    public void revoke(String userUuid, String chatroomUuid) {
        revokeIf(info -> info.userUuid().equals(userUuid), chatroomUuid);
    }

    // 채팅방의 모든 구독 해제 (1:1 대화 차단 시 양쪽 모두)
    public void revokeChatroom(String chatroomUuid) {
        revokeIf(info -> true, chatroomUuid);
    }

    // access token이 만료된 세션의 모든 구독 해제 (세션 자체는 DISCONNECT 시까지 유지되며, 이후 SUBSCRIBE는 거부됨)
    @Scheduled(fixedDelay = 60_000L)
    public void revokeExpired() {
        long now = System.currentTimeMillis();
        sessions.forEach((sessionId, info) -> {
            if (info.expiresAtMillis() <= now) {
                revokeSession(sessionId, null);
            }
        });
    }

    private void revokeIf(Predicate<SessionInfo> target, String chatroomUuid) {
        sessions.forEach((sessionId, info) -> {
            if (target.test(info)) {
                revokeSession(sessionId, chatroomUuid);
            }
        });
    }

    private void revokeSession(String sessionId, String chatroomUuid) {
        Map<String, String> bySession = subscriptions.get(sessionId);
        if (bySession == null) {
            return;
        }
        List<String> subscriptionIds = new ArrayList<>();
        bySession.forEach((subscriptionId, subscribed) -> {
            if (chatroomUuid == null || chatroomUuid.equals(subscribed)) {
                subscriptionIds.add(subscriptionId);
            }
        });
        for (String subscriptionId : subscriptionIds) {
            bySession.remove(subscriptionId);
            unsubscribe(sessionId, subscriptionId);
        }
    }

    // 클라이언트가 UNSUBSCRIBE를 보낸 것과 동일한 메시지를 inbound 채널로 보내 SimpleBroker의 구독 정보를 제거
    private void unsubscribe(String sessionId, String subscriptionId) {
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
            accessor.setSessionId(sessionId);
            accessor.setSubscriptionId(subscriptionId);
            accessor.setLeaveMutable(true);
            clientInboundChannel.send(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));
        } catch (RuntimeException e) {
            log.warn("STOMP 구독 해제 실패 : session={}, subscription={}", sessionId, subscriptionId, e);
        }
    }
}
