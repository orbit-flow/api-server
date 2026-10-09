package com.backend.orbitflow.domain.notification.service;

import com.backend.orbitflow.domain.notification.dto.response.NotificationResponse;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {

    // 활동 알림은 생성 시점부터 30일이 지나면 만료
    int RETENTION_DAYS = 30;

    void send(User receiver, NotificationType type, User actor, Long targetId, String targetUuid, String content);
    SseEmitter subscribe(User user);
    Page<NotificationResponse> getNotifications(User user, boolean unreadOnly, int page, int size);
    long countUnread(User user);
    NotificationResponse read(User user, Long notificationId);
    void readAll(User user);
    void delete(User user, Long notificationId);
    void deleteExpired();
}
