package com.backend.orbitflow.domain.chat.entity;

import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 대화 참여자 : 참여자 목록에 등록된 사용자만 메시지 열람 가능
// created_at = 참여 시각, 그 이후 메시지만 열람 (나간 뒤 다시 참여해도 과거 메시지는 볼 수 없음)
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chatroom_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"chatroom_id", "user_id"}))
public class ChatroomMember extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chatroom_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Chatroom chatroom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // 안 읽은 메시지 수 계산용 (null이면 참여 이후 모든 메시지가 안 읽음)
    private LocalDateTime lastReadAt;

    public static ChatroomMember of(Chatroom chatroom, User user) {
        return new ChatroomMember(null, chatroom, user, null);
    }

    public void read() {
        this.lastReadAt = LocalDateTime.now();
    }

    public LocalDateTime joinedAt() {
        return getCreatedAt();
    }
}
