package com.backend.orbitflow.domain.team.entity;

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
@Table(name = "team_member_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "role_id"}))
public class TeamMemberRole extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private TeamMember member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private TeamRole role;

    public static TeamMemberRole of(TeamMember member, TeamRole role) {
        return new TeamMemberRole(
                null, member, role
        );
    }
}
