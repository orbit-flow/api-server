package com.backend.orbitflow.domain.notification.listener;

import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

// 작업 트랜잭션이 커밋된 직후(작업이 성공적으로 끝난 직후) 별도 스레드에서 알림 저장·발송
// 수신자·내용은 발행한 쪽이 모두 담아 보내므로 다른 도메인을 조회하지 않음, 트랜잭션 밖에서 발행되면 바로 처리
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(NotificationRequest request) {
        if (request.receivers().isEmpty()) {
            return;
        }
        notificationService.sendAll(request.receivers(), request.type(), request.actor(),
                request.targetId(), request.targetUuid(), request.content(), request.repeatable());
    }
}
