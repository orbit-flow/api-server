package com.backend.orbitflow.domain.notification.entity;

import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 생성 후 30일이 지나면 만료되어 목록에서 제거
// actor_id·target_id·target_uuid·content는 ERD 확장 컬럼 (알림 대상·문구 표시용)
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications",
        indexes = @Index(columnList = "user_id, created_at"))
public class Notification extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 수신자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private boolean isRead;

    // 알림을 발생시킨 사용자 (리마인드 등 시스템 알림은 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    // 이동 대상 : 게시글·투두·카테고리·팔로우 id
    @Column(name = "target_id")
    private Long targetId;

    // 이동 대상 : 팀 uuid (팀 내부 id 비노출)
    @Column(name = "target_uuid", length = 36)
    private String targetUuid;

    @Column(nullable = false)
    private String content;

    public static Notification of(User user, NotificationType type, User actor, Long targetId, String targetUuid, String content) {
        return new Notification(
                null, user, type, false, actor, targetId, targetUuid, content
        );
    }

    public void read() {
        this.isRead = true;
    }
}
