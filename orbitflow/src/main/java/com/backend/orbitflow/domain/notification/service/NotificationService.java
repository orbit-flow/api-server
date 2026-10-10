package com.backend.orbitflow.domain.notification.service;

import com.backend.orbitflow.domain.notification.dto.response.NotificationResponse;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.backend.orbitflow.domain.notification.dto.NotificationDraft;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    // 활동 알림은 생성 시점부터 30일이 지나면 만료
    int RETENTION_DAYS = 30;

    // receiverUuids : 수신자 id → uuid (탈퇴·정지 사용자는 호출 측에서 제외, NotificationRequest 참고)
    void sendAll(Map<Long, String> receiverUuids, NotificationType type, User actor, Long targetId, String targetUuid, String content);
    void sendEach(List<NotificationDraft> drafts);
    SseEmitter subscribe(User user);
    Page<NotificationResponse> getNotifications(User user, boolean unreadOnly, int page, int size);
    long countUnread(User user);
    NotificationResponse read(User user, Long notificationId);
    void readAll(User user);
    void delete(User user, Long notificationId);
    void deleteExpired();
}
