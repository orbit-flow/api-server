package com.backend.orbitflow.domain.notification.facade;

import com.backend.orbitflow.domain.notification.dto.response.NotificationResponse;
import com.backend.orbitflow.domain.notification.service.NotificationService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationService notificationService;
    private final UserService userService;

    public SseEmitter subscribe(AuthUser authUser) {
        return notificationService.subscribe(me(authUser));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(AuthUser authUser, boolean unreadOnly, int page, int size) {
        return PageResponse.from(notificationService.getNotifications(me(authUser), unreadOnly, page, size));
    }

    @Transactional(readOnly = true)
    public long countUnread(AuthUser authUser) {
        return notificationService.countUnread(me(authUser));
    }

    @Transactional
    public NotificationResponse read(AuthUser authUser, Long notificationId) {
        return notificationService.read(me(authUser), notificationId);
    }

    @Transactional
    public void readAll(AuthUser authUser) {
        notificationService.readAll(me(authUser));
    }

    @Transactional
    public void delete(AuthUser authUser, Long notificationId) {
        notificationService.delete(me(authUser), notificationId);
    }

    private User me(AuthUser authUser) {
        return userService.getByUuid(authUser.getUuid());
    }
}
