package com.backend.orbitflow.domain.team.entity;

import com.backend.orbitflow.domain.team.enums.TeamInvitationStatus;
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
@Table(name = "team_invitations")
public class TeamInvitation extends BaseEntity {

    // 대기 중인 초대는 생성 후 7일이 지나면 만료 (상태 컬럼은 PENDING 그대로, 조회·수락 시점에 판단)
    public static final int EXPIRATION_DAYS = 7;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User invitee;

    // 외부 노출용 식별자
    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    // 현재 검증·노출에 사용하지 않음 (기존 DB의 NOT NULL 컬럼 유지를 위해 생성 시 값만 채움)
    @Column(nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamInvitationStatus status;

    public static TeamInvitation of(Team team, User inviter, User invitee, String uuid, String token) {
        return new TeamInvitation(
                null, team, inviter, invitee, uuid, token, TeamInvitationStatus.PENDING
        );
    }

    public boolean isPending() {
        return this.status == TeamInvitationStatus.PENDING;
    }

    public boolean isExpired() {
        return !this.getCreatedAt().plusDays(EXPIRATION_DAYS).isAfter(LocalDateTime.now());
    }

    public boolean isInvitee(User user) {
        return this.invitee.getId().equals(user.getId());
    }

    public void accept() {
        this.status = TeamInvitationStatus.ACCEPTED;
    }

    public void reject() {
        this.status = TeamInvitationStatus.REJECTED;
    }

    public void cancel() {
        this.status = TeamInvitationStatus.CANCELLED;
    }
}
