package com.backend.orbitflow.domain.notification.dto.response;

import com.backend.orbitflow.domain.notification.entity.Notification;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// targetId : 게시글·투두·카테고리·팔로우 id, targetUuid : 팀 uuid (type에 따라 하나만 사용)
public record NotificationResponse(
        Long id,
        NotificationType type,
        String content,
        boolean isRead,
        Actor actor,
        Long targetId,
        String targetUuid,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public record Actor(String uuid, String name, String profileImage) {
    }

    // 일괄 저장(JDBC)한 알림의 실시간 전송용 : 저장된 값으로 직접 생성
    public static NotificationResponse created(Long id, NotificationType type, String content, User actor,
                                               Long targetId, String targetUuid, LocalDateTime createdAt) {
        return new NotificationResponse(
                id, type, content, false,
                actor == null ? null : new Actor(actor.getUuid(), actor.getName(), actor.getProfileImage()),
                targetId, targetUuid, createdAt
        );
    }

    public static NotificationResponse from(Notification notification) {
        User actor = notification.getActor();
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getContent(),
                notification.isRead(),
                actor == null ? null : new Actor(actor.getUuid(), actor.getName(), actor.getProfileImage()),
                notification.getTargetId(),
                notification.getTargetUuid(),
                notification.getCreatedAt()
        );
    }
}
