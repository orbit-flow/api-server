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

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "messages",
        // ERD 확장 : 같은 채팅방에서 같은 전송 요청의 재시도(네트워크 오류 등)로 인한 중복 저장 차단
        uniqueConstraints = @UniqueConstraint(columnNames = {"chatroom_id", "sender_id", "client_message_id"}),
        indexes = @Index(columnList = "chatroom_id, id"))
public class Message extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chatroom_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Chatroom chatroom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // ERD 확장 : 클라이언트가 생성한 전송 요청 id (전송 중 상태 표시·재시도 식별)
    @Column(name = "client_message_id", length = 64)
    private String clientMessageId;

    // ERD 확장 : 모든 참여자 대상 전송 취소 시각
    @Column(name = "unsent_at")
    private LocalDateTime unsentAt;

    public static Message of(Chatroom chatroom, User sender, String content, String clientMessageId) {
        return new Message(null, chatroom, sender, content, clientMessageId, null);
    }

    public boolean isSender(User user) {
        return this.sender.getId().equals(user.getId());
    }

    public boolean isUnsent() {
        return this.unsentAt != null;
    }

    public void unsend() {
        this.unsentAt = LocalDateTime.now();
    }
}
