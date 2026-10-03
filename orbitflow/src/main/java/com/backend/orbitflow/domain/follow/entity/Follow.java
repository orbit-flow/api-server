package com.backend.orbitflow.domain.follow.entity;

import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followee_id")
    private User followee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id")
    private User follower;

    @Enumerated(EnumType.STRING)
    private FollowState state;

    public static Follow of(User followee, User follower, FollowState state) {
        return new Follow(
                null,
                followee,
                follower,
                state
        );
    }

    public void acceptFollow() {
        this.state = FollowState.ACCEPTED;
    }

}
