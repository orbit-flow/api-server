package com.backend.orbitflow.domain.category.repository;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.entity.CategoryPermission;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import com.backend.orbitflow.domain.category.dto.response.CategoryPermissionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryPermissionRepository extends JpaRepository<CategoryPermission, Long> {

    // 열람 허용 대상 페이지 (역할 → 팀원 순, 응답 항목으로 바로 조회)
    @Query(value = """
            select new com.backend.orbitflow.domain.category.dto.response.CategoryPermissionResponse(
                r.id, r.name, r.color, u.uuid, u.name, m.nickname)
            from CategoryPermission cp
            left join cp.role r
            left join cp.member m
            left join m.user u
            where cp.category = :category
            order by case when r.id is null then 1 else 0 end, cp.id asc
            """,
            countQuery = "select count(cp) from CategoryPermission cp where cp.category = :category")
    Page<CategoryPermissionResponse> findPageByCategory(@Param("category") Category category, Pageable pageable);

    // member가 직접 또는 보유 역할로 열람 허용된 팀 카테고리 id 목록
    @Query("""
            select distinct cp.category.id from CategoryPermission cp
            where cp.category.team = :team
              and (cp.member = :member
                   or cp.role in (select mr.role from TeamMemberRole mr where mr.member = :member))
            """)
    Set<Long> findAllowedCategoryIds(@Param("team") Team team, @Param("member") TeamMember member);


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CategoryPermission cp where cp.category = :category")
    void deleteAllByCategory(@Param("category") Category category);

    // 팀 삭제 시 팀원 지정 권한 정리 (구성원 소속이 모두 해제되므로, 역할 지정 권한은 복구를 위해 유지)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CategoryPermission cp where cp.member in (select m from TeamMember m where m.team = :team)")
    void deleteAllMemberPermissionsByTeam(@Param("team") Team team);
}
