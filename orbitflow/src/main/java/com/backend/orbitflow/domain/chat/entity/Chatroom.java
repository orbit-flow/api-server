package com.backend.orbitflow.domain.chat.entity;

import com.backend.orbitflow.domain.chat.enums.ChatroomType;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// team이 null이면 개인 채팅방, 있으면 팀 채팅방 (팀당 여러 개)
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chatrooms")
public class Chatroom extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외부 노출용 식별자
    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatroomType type;

    @Column(length = 100)
    private String title;

    // ERD 확장 : DIRECT 대화의 두 사용자 키("작은 id:큰 id"), unique로 같은 두 사용자의 1:1 대화 중복 생성 차단
    @Column(name = "direct_key", unique = true, length = 50)
    private String directKey;

    public static Chatroom direct(String uuid, Long userIdA, Long userIdB) {
        return new Chatroom(null, uuid, null, ChatroomType.DIRECT, null, directKey(userIdA, userIdB));
    }

    public static Chatroom group(String uuid, Team team, String title) {
        return new Chatroom(null, uuid, team, ChatroomType.GROUP, title, null);
    }

    public static String directKey(Long userIdA, Long userIdB) {
        return Math.min(userIdA, userIdB) + ":" + Math.max(userIdA, userIdB);
    }

    public boolean isDirect() {
        return this.type == ChatroomType.DIRECT;
    }

    public boolean isTeamChatroom() {
        return this.team != null;
    }
}
