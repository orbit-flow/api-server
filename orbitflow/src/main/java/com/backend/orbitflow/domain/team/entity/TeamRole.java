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
@Table(name = "team_roles")
public class TeamRole extends BaseEntity {

    public static final String DEFAULT_ROLE_NAME = "멤버";
    public static final String DEFAULT_COLOR = "none";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String color;

    // 표시 순서 (값이 클수록 상위)
    @Column(nullable = false)
    private int priority;

    // 초대 수락 시 자동 부여되는 역할 (팀당 1개)
    @Column(nullable = false)
    private boolean isDefault;

    // TeamPermission 비트 합
    @Column(nullable = false)
    private int permissionsMask;

    public static TeamRole of(Team team, String name, String color, int priority, int permissionsMask) {
        return new TeamRole(
                null, team, name, color == null ? DEFAULT_COLOR : color, priority, false, permissionsMask
        );
    }

    public static TeamRole defaultRole(Team team) {
        return new TeamRole(
                null, team, DEFAULT_ROLE_NAME, DEFAULT_COLOR, 0, true, 0
        );
    }

    public void updateRole(String name, String color, int priority, int permissionsMask) {
        this.name = name;
        this.color = color == null ? DEFAULT_COLOR : color;
        this.priority = priority;
        this.permissionsMask = permissionsMask;
    }

    public void updateDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
