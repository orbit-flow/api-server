package com.backend.orbitflow.domain.category.entity;

import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// PRIVATE 팀 카테고리의 열람 허용 대상 : 역할(role) 또는 특정 팀원(member) 중 하나 (XOR)
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "category_permissions",
        indexes = {
                @Index(columnList = "category_id, role_id"),
                @Index(columnList = "category_id, member_id")
        })
public class CategoryPermission extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private TeamRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private TeamMember member;

    public static CategoryPermission ofRole(Category category, TeamRole role) {
        return new CategoryPermission(
                null, category, role, null
        );
    }

    public static CategoryPermission ofMember(Category category, TeamMember member) {
        return new CategoryPermission(
                null, category, null, member
        );
    }
}
