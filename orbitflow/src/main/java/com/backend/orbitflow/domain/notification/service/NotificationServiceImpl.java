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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import com.backend.orbitflow.global.util.JdbcBulkInserter;
import com.backend.orbitflow.domain.notification.dto.NotificationDraft;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String EVENT_NAME = "notification";

    // 같은 (유형, 행위자, 대상)의 알림은 수신자에게 한 번만 발송 : 좋아요 취소 후 재좋아요, 언팔로우 후 재팔로우, 완료 취소 후 재완료 등 토글성 활동
    // 만료(30일)로 삭제된 알림은 다시 발송될 수 있음
    private static final Set<NotificationType> ONCE_TYPES = EnumSet.of(
            NotificationType.LIKE, NotificationType.SOCIAL, NotificationType.TODO_COMPLETED);

    private final NotificationRepository notificationRepository;
    private final JdbcBulkInserter jdbcBulkInserter;
    private final NotificationEmitterRepository emitterRepository;
    private final BlockService blockService;

    // 본인 활동, 차단 관계, 이미 발송한 토글성 활동에는 알림을 생성하지 않음
    // 수신자 id·uuid만으로 같은 알림 일괄 발송 (팔로워 등 대량 수신자를 엔티티로 읽지 않도록), 차단 관계를 수신자 묶음 단위로 한 번에 확인 (수신자마다 조회하지 않음)
    // repeatable이면 토글성 활동 중복 제거를 하지 않음 (팔로우 요청 수락 등 일회성 결과)
    public void sendAll(Map<Long, String> receiverUuids, NotificationType type, User actor, Long targetId, String targetUuid, String content, boolean repeatable) {
        List<Long> targets = receiverUuids.keySet().stream()
                .filter(receiverId -> actor == null || !actor.getId().equals(receiverId))
                .toList();
        if (targets.isEmpty()) {
            return;
        }
        Set<Long> blocked = actor == null
                ? Set.of()
                : blockService.findBlockedUserIdsAmong(actor, targets);
        // 이미 발송한 수신자도 수신자 묶음 단위로 한 번에 확인
        Set<Long> sent = actor == null || repeatable || !ONCE_TYPES.contains(type)
                ? Set.of()
                : notificationRepository.findSentUserIds(targets, type, actor, onceTargetId(type, targetId));
        List<Long> receiverIds = targets.stream()
                .filter(receiverId -> !blocked.contains(receiverId))
                .filter(receiverId -> !sent.contains(receiverId))
                .toList();
        // 모든 수신자의 알림 내용이 같으므로 내용만 담은 draft 하나를 공유 (수신자는 receiverIds·uuid 목록 사용)
        insertAndPush(receiverIds, receiverIds.stream().map(receiverUuids::get).toList(),
                Collections.nCopies(receiverIds.size(), new NotificationDraft(null, type, targetId, targetUuid, content)), actor);
    }

    // 수신자마다 내용이 다른 알림 일괄 발송 (리마인드 등 행위자 없는 알림), 탈퇴·정지 사용자 제외
    public void sendEach(List<NotificationDraft> drafts) {
        insertAndPush(drafts.stream()
                .filter(draft -> draft.receiver().getDeletedAt() == null && draft.receiver().getStatus() != UserStatus.BANNED)
                .toList(), null);
    }

    // 팔로우는 언팔로우 후 다시 팔로우하면 follow id가 바뀌므로 대상과 무관하게 판정
    private Long onceTargetId(NotificationType type, Long targetId) {
        return type == NotificationType.SOCIAL ? null : targetId;
    }

    private void insertAndPush(List<NotificationDraft> drafts, User actor) {
        insertAndPush(drafts.stream().map(draft -> draft.receiver().getId()).toList(),
                drafts.stream().map(draft -> draft.receiver().getUuid()).toList(), drafts, actor);
    }

    // 알림 수와 무관하게 INSERT 1회 (JDBC 배치), 저장이 확정된 뒤 실시간 전송
    // i번째 알림의 수신자는 receiverIds·receiverUuids의 i번째, 내용은 drafts의 i번째 (draft의 receiver는 사용하지 않음)
    private void insertAndPush(List<Long> receiverIds, List<String> receiverUuids, List<NotificationDraft> drafts, User actor) {
        if (receiverIds.isEmpty()) {
            return;
        }
        Long actorId = actor == null ? null : actor.getId();
        List<Object[]> rows = new ArrayList<>(receiverIds.size());
        for (int i = 0; i < receiverIds.size(); i++) {
            NotificationDraft draft = drafts.get(i);
            rows.add(new Object[]{receiverIds.get(i), draft.type(), false, actorId,
                    draft.targetId(), draft.targetUuid(), draft.content()});
        }
        List<Long> ids = jdbcBulkInserter.insert("notifications",
                List.of("user_id", "type", "is_read", "actor_id", "target_id", "target_uuid", "content"), rows);
        LocalDateTime now = LocalDateTime.now();
        List<Runnable> pushes = new ArrayList<>();
        for (int i = 0; i < receiverIds.size(); i++) {
            NotificationDraft draft = drafts.get(i);
            String uuid = receiverUuids.get(i);
            NotificationResponse response = NotificationResponse.created(
                    ids.get(i), draft.type(), draft.content(), actor, draft.targetId(), draft.targetUuid(), now);
            pushes.add(() -> emitterRepository.send(uuid, EVENT_NAME, response));
        }
        afterCommit(() -> pushes.forEach(Runnable::run));
    }

    public SseEmitter subscribe(User user) {
        return emitterRepository.connect(user.getUuid());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(User user, boolean unreadOnly, int page, int size) {
        return notificationRepository.findAllByUser(user, threshold(), unreadOnly, PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100)))
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
