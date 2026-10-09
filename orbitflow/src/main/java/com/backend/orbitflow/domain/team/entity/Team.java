package com.backend.orbitflow.domain.team.entity;

import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.SoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "teams")
public class Team extends SoftDeleteEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외부 노출용 식별자 (내부 id는 노출하지 않음)
    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(nullable = false, length = 50)
    private String name;

    // null이면 FE에서 identicon 렌더링
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    public static Team of(String uuid, String name, String icon, User owner) {
        return new Team(
                null, uuid, name, icon, owner
        );
    }

    public void updateTeamInfo(String name, String icon) {
        this.name = name;
        this.icon = icon;
    }

    public boolean isOwner(User user) {
        return this.owner.getId().equals(user.getId());
    }
}
