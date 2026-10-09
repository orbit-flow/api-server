package com.backend.orbitflow.domain.team.entity;

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
@Table(name = "team_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "user_id"}))
public class TeamMember extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // 팀 내 별칭
    @Column(length = 50)
    private String nickname;

    // 팀 전용 소개글
    private String bio;

    public static TeamMember of(Team team, User user) {
        return new TeamMember(
                null, team, user, null, null
        );
    }

    public void updateProfile(String nickname, String bio) {
        this.nickname = nickname;
        this.bio = bio;
    }
}
