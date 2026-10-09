package com.backend.orbitflow.domain.follow.entity;

import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "follows",
        uniqueConstraints = @UniqueConstraint(columnNames = {"followee_id", "follower_id"}))
public class Follow extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User followee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User follower;

    // NOT_FOLLOW는 응답 전용 상태이므로 DB에는 PENDING, ACCEPTED만 저장
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "enum('PENDING','ACCEPTED')")
    private FollowState state;

    @Column(name = "is_notification_enabled", nullable = false)
    private boolean notificationEnabled;

    // 비밀계정이면 PENDING, 공개계정이면 즉시 ACCEPTED
    public static Follow of(User followee, User follower) {
        return new Follow(
                null,
                followee,
                follower,
                followee.isPrivate() ? FollowState.PENDING : FollowState.ACCEPTED,
                true
        );
    }

    public void acceptFollow() {
        this.state = FollowState.ACCEPTED;
    }

    public void toggleNotification() {
        this.notificationEnabled = !this.notificationEnabled;
    }

    public boolean isPending() {
        return this.state == FollowState.PENDING;
    }
}
