package com.backend.orbitflow.domain.category.entity;

import com.backend.orbitflow.domain.category.enums.Visibility;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 개인 카테고리(user) 또는 팀 카테고리(team) 중 하나에만 소속 (XOR)
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "categories",
        indexes = @Index(columnList = "user_id, team_id"))
public class Category extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    public static Category personal(User user, String name, String color, Visibility visibility) {
        return new Category(
                null, null, user, name, color, visibility == null ? Visibility.PUBLIC : visibility
        );
    }

    public static Category team(Team team, String name, String color, Visibility visibility) {
        return new Category(
                null, team, null, name, color, visibility == null ? Visibility.PUBLIC : visibility
        );
    }

    public void updateCategory(String name, String color, Visibility visibility) {
        this.name = name;
        this.color = color;
        this.visibility = visibility == null ? Visibility.PUBLIC : visibility;
    }

    public boolean isTeamCategory() {
        return this.team != null;
    }

    public boolean isOwnedBy(User user) {
        return this.user != null && this.user.getId().equals(user.getId());
    }

    // 같은 소유자(같은 사용자 또는 같은 팀)의 카테고리인지
    public boolean isSameOwner(Category other) {
        if (isTeamCategory()) {
            return other.isTeamCategory() && this.team.getId().equals(other.team.getId());
        }
        return !other.isTeamCategory() && this.user.getId().equals(other.user.getId());
    }
}
