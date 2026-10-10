package com.backend.orbitflow.domain.category.repository;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Set;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 개인 카테고리 c(소유자 cu)를 :viewerId 사용자가 볼 수 있는지 (CategoryAuthorityService.filterViewablePersonal과 같은 규칙)
    // 본인 전체, 차단 관계면 없음, 수락된 팔로워면 PUBLIC·FOLLOWER, 아니면 공개계정의 PUBLIC만 (비밀계정은 PUBLIC도 FOLLOWER로 제한)
    String PERSONAL_CATEGORY_VIEWABLE = """
            (c.user_id = :viewerId
             or (not exists (select 1 from blocks b
                             where (b.blocker_id = c.user_id and b.blockee_id = :viewerId)
                                or (b.blocker_id = :viewerId and b.blockee_id = c.user_id))
                 and ((c.visibility = 'PUBLIC' and cu.is_private = 0)
                      or (c.visibility in ('PUBLIC', 'FOLLOWER')
                          and exists (select 1 from follows f
                                      where f.followee_id = c.user_id and f.follower_id = :viewerId and f.status = 'ACCEPTED')))))
            """;

    // 팀 카테고리 c를 :viewerId 사용자가 볼 수 있는지 (CategoryAuthorityService.filterViewableTeam과 같은 규칙)
    // PUBLIC 누구나, FOLLOWER 팀 구성원, PRIVATE 팀 소유자·카테고리 관리 권한(비트 8) 보유자·본인 또는 본인 역할에 열람 허용된 경우
    String TEAM_CATEGORY_VIEWABLE = """
            (c.visibility = 'PUBLIC'
             or (c.visibility = 'FOLLOWER'
                 and exists (select 1 from team_members vm where vm.team_id = c.team_id and vm.user_id = :viewerId))
             or (c.visibility = 'PRIVATE'
                 and (exists (select 1 from teams vt where vt.id = c.team_id and vt.owner_id = :viewerId)
                      or exists (select 1 from team_members vm
                                 join team_member_roles vmr on vmr.member_id = vm.id
                                 join team_roles vr on vr.id = vmr.role_id
                                 where vm.team_id = c.team_id and vm.user_id = :viewerId and (vr.permissions_mask & 8) <> 0)
                      or exists (select 1 from category_permissions cp
                                 join team_members vm on vm.id = cp.member_id
                                 where cp.category_id = c.id and vm.user_id = :viewerId)
                      or exists (select 1 from category_permissions cp
                                 join team_member_roles vmr on vmr.role_id = cp.role_id
                                 join team_members vm on vm.id = vmr.member_id
                                 where cp.category_id = c.id and vm.user_id = :viewerId))))
            """;


    @Query("select c from Category c left join fetch c.user left join fetch c.team where c.id = :id")
    Optional<Category> findWithOwnerById(@Param("id") Long id);

    Page<Category> findAllByUserOrderByCreatedAtAscIdAsc(User user, Pageable pageable);

    // 다른 사용자의 개인 카테고리 중 조회 사용자가 볼 수 있는 것만
    @Query(nativeQuery = true,
            value = "select c.* from categories c join users cu on cu.id = c.user_id where c.user_id = :ownerId and "
                    + PERSONAL_CATEGORY_VIEWABLE + " order by c.created_at asc, c.id asc",
            countQuery = "select count(*) from categories c join users cu on cu.id = c.user_id where c.user_id = :ownerId and "
                    + PERSONAL_CATEGORY_VIEWABLE)
    Page<Category> findViewablePersonal(@Param("ownerId") Long ownerId, @Param("viewerId") Long viewerId, Pageable pageable);

    // 팀 카테고리 중 조회 사용자가 볼 수 있는 것만
    @Query(nativeQuery = true,
            value = "select c.* from categories c where c.team_id = :teamId and " + TEAM_CATEGORY_VIEWABLE
                    + " order by c.created_at asc, c.id asc",
            countQuery = "select count(*) from categories c where c.team_id = :teamId and " + TEAM_CATEGORY_VIEWABLE)
    Page<Category> findViewableTeam(@Param("teamId") Long teamId, @Param("viewerId") Long viewerId, Pageable pageable);

    // 팀 카테고리를 볼 수 있는 팀 구성원의 사용자 id (구성원마다 권한을 조회하지 않음)
    // PUBLIC·FOLLOWER 모든 구성원, PRIVATE 소유자·카테고리 관리 권한 보유자·열람 허용된 구성원
    @Query(nativeQuery = true, value = """
            select m.user_id from team_members m
            join categories c on c.team_id = m.team_id
            where c.id = :categoryId
              and (c.visibility <> 'PRIVATE'
                   or exists (select 1 from teams vt where vt.id = c.team_id and vt.owner_id = m.user_id)
                   or exists (select 1 from team_member_roles vmr
                              join team_roles vr on vr.id = vmr.role_id
                              where vmr.member_id = m.id and (vr.permissions_mask & 8) <> 0)
                   or exists (select 1 from category_permissions cp where cp.category_id = c.id and cp.member_id = m.id)
                   or exists (select 1 from category_permissions cp
                              join team_member_roles vmr on vmr.role_id = cp.role_id
                              where cp.category_id = c.id and vmr.member_id = m.id))
            """)
    Set<Long> findViewerUserIds(@Param("categoryId") Long categoryId);
}
