package com.backend.orbitflow.domain.notification.listener;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 작업 트랜잭션이 커밋된 직후(작업이 성공적으로 끝난 직후) 별도 스레드에서 알림 저장·발송
// 수신자·내용은 발행한 쪽이 모두 담아 보내므로 차단 관계 외에는 다른 도메인을 조회하지 않음, 트랜잭션 밖에서 발행되면 바로 처리
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final BlockService blockService;

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(NotificationRequest request) {
        if (request.receivers().isEmpty()) {
            return;
        }
        Map<Long, String> receivers = excludeBlocked(request);
        notificationService.sendAll(receivers, request.type(), request.actor(),
                request.targetId(), request.targetUuid(), request.content(), request.repeatable());
    }

    // 행위자와 차단 관계(어느 방향이든)인 수신자 제외, 수신자 묶음 단위로 한 번에 확인 (수신자마다 조회하지 않음)
    // 본인 활동은 NotificationService에서 제외하므로 조회 대상에서도 뺌
    private Map<Long, String> excludeBlocked(NotificationRequest request) {
        if (request.actor() == null) {
            return request.receivers();
        }
        List<Long> targets = request.receivers().keySet().stream()
                .filter(receiverId -> !request.actor().getId().equals(receiverId))
                .toList();
        if (targets.isEmpty()) {
            return request.receivers();
        }
        Set<Long> blocked = blockService.findBlockedUserIdsAmong(request.actor(), targets);
        Map<Long, String> receivers = new LinkedHashMap<>(request.receivers());
        receivers.keySet().removeAll(blocked);
        return receivers;
    }
}
