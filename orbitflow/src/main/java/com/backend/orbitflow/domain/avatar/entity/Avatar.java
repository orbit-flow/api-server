package com.backend.orbitflow.domain.avatar.entity;

import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사용자당 1개, 가입 시 기본 아바타와 초기 포인트 지급
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "avatars")
public class Avatar extends BaseEntity {

    public static final int INITIAL_POINT = 50_000;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private long exp;

    // 1포인트 단위 정수, 회수 시 음수 가능
    @Column(nullable = false)
    private int point;

    public static Avatar of(User user) {
        return new Avatar(
                null, user, 1, 0L, INITIAL_POINT
        );
    }

    public void addPoint(int amount) {
        this.point += amount;
    }

    public boolean hasPoint(int amount) {
        return this.point >= amount;
    }
}
