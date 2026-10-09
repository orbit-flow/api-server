package com.backend.orbitflow.domain.team.entity;

import com.backend.orbitflow.domain.team.enums.TeamInvitationStatus;
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
@Table(name = "team_invitations")
public class TeamInvitation extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    // 외부 노출용 식별자
    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    // 초대 링크(이메일 등) 검증용 토큰
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
