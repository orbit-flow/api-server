package com.backend.orbitflow.domain.notification.event;

import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.UserStatus;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 알림 발송 요청 : 작업을 마친 쪽(Facade 등)이 수신자·내용을 모두 담아 발행하고, 커밋 후 NotificationEventListener가 저장·실시간 전송
 *
 * <ul>
 *   <li>receivers : 수신자 id → uuid (탈퇴·정지 사용자는 만드는 시점에 제외)</li>
 *   <li>actor : 행위자 (없으면 null), 본인·차단 관계·이미 발송한 토글성 활동 제외는 NotificationService에서 판정</li>
 * </ul>
 */
public record NotificationRequest(
        Map<Long, String> receivers,
        NotificationType type,
        User actor,
        Long targetId,
        String targetUuid,
        String content,
        // true면 같은 (유형, 행위자, 대상) 알림을 이미 보냈어도 다시 발송 (토글성 활동 중복 제거에서 제외)
        boolean repeatable
) {

    public NotificationRequest(Map<Long, String> receivers, NotificationType type, User actor, Long targetId, String targetUuid, String content) {
        this(receivers, type, actor, targetId, targetUuid, content, false);
    }

    public static NotificationRequest to(User receiver, NotificationType type, User actor, Long targetId, String targetUuid, String content) {
        return toAll(List.of(receiver), type, actor, targetId, targetUuid, content);
    }

    public static NotificationRequest toAll(Collection<User> receivers, NotificationType type, User actor, Long targetId, String targetUuid, String content) {
        Map<Long, String> receiverUuids = new LinkedHashMap<>();
        receivers.stream()
                .filter(receiver -> receiver.getDeletedAt() == null && receiver.getStatus() != UserStatus.BANNED)
                .forEach(receiver -> receiverUuids.put(receiver.getId(), receiver.getUuid()));
        return new NotificationRequest(receiverUuids, type, actor, targetId, targetUuid, content);
    }

    // 토글이 아닌 일회성 결과(팔로우 요청 수락 등) : 같은 유형의 이전 알림과 무관하게 발송
    public NotificationRequest asRepeatable() {
        return new NotificationRequest(receivers, type, actor, targetId, targetUuid, content, true);
    }
}
