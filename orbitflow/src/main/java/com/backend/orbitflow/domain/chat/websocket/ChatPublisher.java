package com.backend.orbitflow.domain.chat.websocket;

import com.backend.orbitflow.domain.chat.dto.response.ChatEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 채팅방 구독자에게 실시간 전달 : DB 반영이 확정된 뒤에만 전송 (롤백된 메시지가 보이지 않도록)
// TODO: 다중 인스턴스 배포 시 외부 브로커(RabbitMQ·Redis Pub/Sub 등) 릴레이 필요 (현재 SimpleBroker는 인스턴스 메모리)
@Component
@RequiredArgsConstructor
public class ChatPublisher {

    public static final String CHATROOM_TOPIC = "/sub/chatrooms/";

    private final SimpMessagingTemplate messagingTemplate;

    public void publishAfterCommit(String chatroomUuid, ChatEvent event) {
        Runnable send = () -> messagingTemplate.convertAndSend(CHATROOM_TOPIC + chatroomUuid, event);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send.run();
            }
        });
    }
}
