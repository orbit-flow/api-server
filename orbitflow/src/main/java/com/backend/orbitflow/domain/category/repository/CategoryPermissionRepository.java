package com.backend.orbitflow.domain.category.repository;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.entity.CategoryPermission;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import com.backend.orbitflow.domain.category.dto.CategoryGrant;
import com.backend.orbitflow.domain.user.entity.User;

public interface CategoryPermissionRepository extends JpaRepository<CategoryPermission, Long> {

    @Query("""
            select cp from CategoryPermission cp
            left join fetch cp.role
            left join fetch cp.member m
            left join fetch m.user
            where cp.category = :category
            """)
    List<CategoryPermission> findAllWithTargetByCategory(@Param("category") Category category);

    // member가 직접 또는 보유 역할로 열람 허용된 팀 카테고리 id 목록
    @Query("""
            select distinct cp.category.id from CategoryPermission cp
            where cp.category.team = :team
              and (cp.member = :member
                   or cp.role in (select mr.role from TeamMemberRole mr where mr.member = :member))
            """)
    Set<Long> findAllowedCategoryIds(@Param("team") Team team, @Param("member") TeamMember member);

    // 팀의 모든 카테고리 열람 허용 대상 (TeamCategoryAccess)
    @Query("""
            select new com.backend.orbitflow.domain.category.dto.CategoryGrant(c.id, r.id, m.id)
            from CategoryPermission cp
            join cp.category c
            left join cp.role r
            left join cp.member m
            where c.team = :team
            """)
    List<CategoryGrant> findGrantsByTeam(@Param("team") Team team);

    // 사용자 본인 또는 본인 역할에 열람이 허용된 카테고리 (모든 팀, ViewerTeamScope)
    @Query("""
            select distinct c.id
            from CategoryPermission cp
            join cp.category c
            left join cp.member m
            left join cp.role r
            where m.user = :user
               or r in (select mr.role from TeamMemberRole mr where mr.member.user = :user)
            """)
    Set<Long> findAllowedCategoryIdsByUser(@Param("user") User user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CategoryPermission cp where cp.category = :category")
    void deleteAllByCategory(@Param("category") Category category);

    // 팀 삭제 시 팀원 지정 권한 정리 (구성원 소속이 모두 해제되므로, 역할 지정 권한은 복구를 위해 유지)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CategoryPermission cp where cp.member in (select m from TeamMember m where m.team = :team)")
    void deleteAllMemberPermissionsByTeam(@Param("team") Team team);
}
