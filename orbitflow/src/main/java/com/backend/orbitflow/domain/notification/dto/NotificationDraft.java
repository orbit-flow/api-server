package com.backend.orbitflow.domain.notification.dto;

import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.user.entity.User;

// 수신자마다 내용이 다른 알림 한 건 (리마인드 등 일괄 저장용, 행위자 없음)
public record NotificationDraft(User receiver, NotificationType type, Long targetId, String targetUuid, String content) {
}
