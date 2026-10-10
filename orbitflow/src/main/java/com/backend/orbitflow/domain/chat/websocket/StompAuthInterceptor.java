package com.backend.orbitflow.domain.chat.websocket;

import com.backend.orbitflow.domain.chat.repository.ChatroomMemberRepository;
import com.backend.orbitflow.domain.suspension.cache.SuspendedUserCache;
import com.backend.orbitflow.global.security.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;

/**
 * STOMP 인증·인가
 * <ul>
 *   <li>CONNECT : Authorization 헤더(Bearer access token) 검증 (로그아웃 토큰·정지 계정 거부)</li>
 *   <li>SUBSCRIBE : /sub/chatrooms/{uuid} 만 허용하고, 현재 참여자인지 확인</li>
 *   <li>SEND : 메시지 전송은 REST API로만 허용 (전송 결과를 응답으로 확인하기 위함)</li>
 *   <li>구독 이후 권한을 잃으면(퇴장·추방·차단·정지·토큰 만료 등) ChatSubscriptionRegistry가 구독을 해제</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final SuspendedUserCache suspendedUserCache;
    private final ChatroomMemberRepository chatroomMemberRepository;
    private final ChatSubscriptionRegistry subscriptionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> accessor.setUser(authenticate(accessor));
            case SUBSCRIBE -> authorizeSubscribe(accessor);
            case UNSUBSCRIBE -> subscriptionRegistry.removeSubscription(accessor.getSessionId(), accessor.getSubscriptionId());
            case DISCONNECT -> subscriptionRegistry.removeSession(accessor.getSessionId());
            case SEND -> throw new MessagingException("메시지 전송은 REST API를 사용해야 합니다.");
            default -> {
            }
        }
        return message;
    }

    private Principal authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(JwtProvider.AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(JwtProvider.BEARER_PREFIX)) {
            throw new MessagingException("인증 정보가 없습니다.");
        }
        String token = header.substring(JwtProvider.BEARER_PREFIX.length());
        try {
            Claims claims = jwtProvider.getUserInfoFromToken(token);
            // auth 클레임이 없는 토큰(refresh token)은 access token이 아님
            if (claims.get(JwtProvider.AUTHORIZATION_KEY, String.class) == null) {
                throw new MessagingException("유효하지 않은 토큰입니다.");
            }
            String uuid = claims.getSubject();
            // 로그아웃 토큰 여부와 정지 여부를 한 번의 Redis 조회로 확인 (블랙리스트는 유효한 access token만 등록됨)
            SuspendedUserCache.AccessStatus access = suspendedUserCache.checkAccess(token, uuid);
            if (access.loggedOut()) {
                throw new MessagingException("로그아웃된 토큰입니다.");
            }
            if (access.suspended()) {
                throw new MessagingException("정지된 계정입니다.");
            }
            // 세션별 access token 만료 시각 기록 : 만료 이후에는 구독이 해제되고 새 구독도 거부
            subscriptionRegistry.registerSession(accessor.getSessionId(), uuid, claims.getExpiration().getTime());
            return () -> uuid;
        } catch (JwtException | IllegalArgumentException e) {
            throw new MessagingException("유효하지 않은 토큰입니다.");
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (user == null || destination == null || !destination.startsWith(ChatPublisher.CHATROOM_TOPIC)) {
            throw new MessagingException("구독할 수 없는 경로입니다.");
        }
        String chatroomUuid = destination.substring(ChatPublisher.CHATROOM_TOPIC.length());
        if (!chatroomMemberRepository.existsByChatroomUuidAndUserUuid(chatroomUuid, user.getName())) {
            throw new MessagingException("대화 참여자만 구독할 수 있습니다.");
        }
        // CONNECT 이후 만료·정지된 세션은 새 구독 불가
        if (subscriptionRegistry.isExpired(accessor.getSessionId()) || suspendedUserCache.isSuspended(user.getName())) {
            throw new MessagingException("인증이 만료되었거나 정지된 계정입니다.");
        }
        subscriptionRegistry.addSubscription(accessor.getSessionId(), accessor.getSubscriptionId(), chatroomUuid);
    }
}
