package com.backend.orbitflow.domain.notification.service;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.notification.dto.response.NotificationResponse;
import com.backend.orbitflow.domain.notification.entity.Notification;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.error.NotificationErrorCode;
import com.backend.orbitflow.domain.notification.repository.NotificationRepository;
import com.backend.orbitflow.domain.notification.sse.NotificationEmitterRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.UserStatus;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String EVENT_NAME = "notification";

    private final NotificationRepository notificationRepository;
    private final NotificationEmitterRepository emitterRepository;
    private final BlockService blockService;

    // 본인 활동, 차단 관계, 탈퇴·정지 계정에는 알림을 생성하지 않음
    public void send(User receiver, NotificationType type, User actor, Long targetId, String targetUuid, String content) {
        if (receiver.getDeletedAt() != null || receiver.getStatus() == UserStatus.BANNED) {
            return;
        }
        if (actor != null) {
            if (actor.getId().equals(receiver.getId()) || blockService.isBlocked(actor, receiver)) {
                return;
            }
        }
        Notification notification = notificationRepository.save(
                Notification.of(receiver, type, actor, targetId, targetUuid, content)
        );
        NotificationResponse response = NotificationResponse.from(notification);
        String uuid = receiver.getUuid();
        // 저장이 확정된 뒤 실시간 전송
        afterCommit(() -> emitterRepository.send(uuid, EVENT_NAME, response));
    }

    // 여러 수신자에게 같은 알림 : 차단 관계를 수신자 묶음 단위로 한 번에 확인 (수신자마다 조회하지 않음)
    public void sendAll(Collection<User> receivers, NotificationType type, User actor, Long targetId, String targetUuid, String content) {
        List<User> targets = receivers.stream()
                .filter(receiver -> receiver.getDeletedAt() == null && receiver.getStatus() != UserStatus.BANNED)
                .filter(receiver -> actor == null || !actor.getId().equals(receiver.getId()))
                .toList();
        if (targets.isEmpty()) {
            return;
        }
        Set<Long> blocked = actor == null
                ? Set.of()
                : blockService.findBlockedUserIdsAmong(actor, targets.stream().map(User::getId).toList());
        List<Notification> notifications = notificationRepository.saveAll(targets.stream()
                .filter(receiver -> !blocked.contains(receiver.getId()))
                .map(receiver -> Notification.of(receiver, type, actor, targetId, targetUuid, content))
                .toList());
        List<Runnable> pushes = notifications.stream()
                .map(notification -> {
                    String uuid = notification.getUser().getUuid();
                    NotificationResponse response = NotificationResponse.from(notification);
                    return (Runnable) () -> emitterRepository.send(uuid, EVENT_NAME, response);
                })
                .toList();
        // 저장이 확정된 뒤 실시간 전송
        afterCommit(() -> pushes.forEach(Runnable::run));
    }

    public SseEmitter subscribe(User user) {
        return emitterRepository.connect(user.getUuid());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(User user, boolean unreadOnly, int page, int size) {
        return notificationRepository.findAllByUser(user, threshold(), unreadOnly, PageRequest.of(Math.max(page - 1, 0), size))
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long countUnread(User user) {
        return notificationRepository.countUnread(user, threshold());
    }

    public NotificationResponse read(User user, Long notificationId) {
        Notification notification = getNotification(user, notificationId);
        notification.read();
        return NotificationResponse.from(notification);
    }

    public void readAll(User user) {
        notificationRepository.markAllRead(user);
    }

    public void delete(User user, Long notificationId) {
        notificationRepository.delete(getNotification(user, notificationId));
    }

    public void deleteExpired() {
        int deleted = notificationRepository.deleteAllExpired(threshold());
        log.info("만료 알림 삭제 완료: {}건", deleted);
    }

    // 다른 사용자의 알림·만료된 알림은 존재하지 않는 것으로 처리
    private Notification getNotification(User user, Long notificationId) {
        return notificationRepository.findByIdAndUser(notificationId, user)
                .filter(notification -> notification.getCreatedAt().isAfter(threshold()))
                .orElseThrow(() -> new CommonException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

    private LocalDateTime threshold() {
        return LocalDateTime.now().minusDays(RETENTION_DAYS);
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
