package com.backend.orbitflow.domain.follow.repository;

import com.backend.orbitflow.domain.follow.dto.response.FollowListResponse;
import com.backend.orbitflow.domain.follow.entity.Follow;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.Set;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerAndFollowee(User follower, User followee);

    // userIds 중 user를 팔로우 중인(승인된) 사용자 id
    @Query("""
            select f.follower.id from Follow f
            where f.followee = :user
              and f.state = com.backend.orbitflow.domain.follow.enums.FollowState.ACCEPTED
              and f.follower.id in :userIds
            """)
    Set<Long> findFollowerIdsAmong(@Param("user") User user, @Param("userIds") Collection<Long> userIds);

    // 활동 알림 수신 대상 : 승인된 팔로워 중 해당 계정의 알림을 켠 사용자 (탈퇴·정지 사용자 제외)
    // 팔로워 수와 무관하게 메모리를 쓰지 않도록 발송에 필요한 id·uuid만, afterId 이후 id 순으로 pageable 크기만큼 조회
    @Query("""
            select u.id as id, u.uuid as uuid from Follow f
            join f.follower u
            where f.followee = :user
              and f.state = com.backend.orbitflow.domain.follow.enums.FollowState.ACCEPTED
              and f.notificationEnabled = true
              and u.deletedAt is null
              and u.status <> com.backend.orbitflow.domain.user.enums.UserStatus.BANNED
              and u.id > :afterId
            order by u.id asc
            """)
    List<NotifiableFollower> findNotifiableFollowers(@Param("user") User user, @Param("afterId") Long afterId, Pageable pageable);

    interface NotifiableFollower {

        Long getId();
        String getUuid();
    }

    @Query("select f from Follow f join fetch f.follower join fetch f.followee where f.id = :id")
    Optional<Follow> findWithUsersById(@Param("id") Long id);

    // target이 팔로우하는 사용자 목록
    // myState : me -> 목록 사용자 방향의 상태, follows row가 없으면 NOT_FOLLOW로 매핑
    // 탈퇴·정지 사용자와 me와 차단 관계인 사용자는 제외
    @Query(value = """
            select new com.backend.orbitflow.domain.follow.dto.response.FollowListResponse(
                f.id, u.uuid, u.name, u.profileImage,
                case when mf.id is null
                    then com.backend.orbitflow.domain.follow.enums.FollowState.NOT_FOLLOW
                    else mf.state end,
                f.createdAt)
            from Follow f
            join f.followee u
            left join Follow mf on mf.follower = :me and mf.followee = u
            where f.follower = :target
              and f.state = com.backend.orbitflow.domain.follow.enums.FollowState.ACCEPTED
              and u.deletedAt is null
              and u.status <> com.backend.orbitflow.domain.user.enums.UserStatus.BANNED
              and (:keyword is null or u.name like concat('%', :keyword, '%'))
              and not exists (select b.id from Block b
                              where (b.blocker = :me and b.blockee = u) or (b.blocker = u and b.blockee = :me))
            order by f.createdAt desc
            """,
            countQuery = """
            select count(f) from Follow f
            join f.followee u
            where f.follower = :target
              and f.state = com.backend.orbitflow.domain.follow.enums.FollowState.ACCEPTED
              and u.deletedAt is null
              and u.status <> com.backend.orbitflow.domain.user.enums.UserStatus.BANNED
              and (:keyword is null or u.name like concat('%', :keyword, '%'))
              and not exists (select b.id from Block b
                              where (b.blocker = :me and b.blockee = u) or (b.blocker = u and b.blockee = :me))
            """)
    Page<FollowListResponse> findFollowings(
            @Param("me") User me,
            @Param("target") User target,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // target을 팔로우하는 사용자 목록 (state == PENDING이면 받은 팔로우 요청 목록)
    // myState : me -> 목록 사용자 방향의 상태, follows row가 없으면 NOT_FOLLOW로 매핑
    // 탈퇴·정지 사용자와 me와 차단 관계인 사용자는 제외
    @Query(value = """
            select new com.backend.orbitflow.domain.follow.dto.response.FollowListResponse(
                f.id, u.uuid, u.name, u.profileImage,
                case when mf.id is null
                    then com.backend.orbitflow.domain.follow.enums.FollowState.NOT_FOLLOW
                    else mf.state end,
                f.createdAt)
            from Follow f
            join f.follower u
            left join Follow mf on mf.follower = :me and mf.followee = u
            where f.followee = :target
              and f.state = :state
              and u.deletedAt is null
              and u.status <> com.backend.orbitflow.domain.user.enums.UserStatus.BANNED
              and (:keyword is null or u.name like concat('%', :keyword, '%'))
              and not exists (select b.id from Block b
                              where (b.blocker = :me and b.blockee = u) or (b.blocker = u and b.blockee = :me))
            order by f.createdAt desc
            """,
            countQuery = """
            select count(f) from Follow f
            join f.follower u
            where f.followee = :target
              and f.state = :state
              and u.deletedAt is null
              and u.status <> com.backend.orbitflow.domain.user.enums.UserStatus.BANNED
              and (:keyword is null or u.name like concat('%', :keyword, '%'))
              and not exists (select b.id from Block b
                              where (b.blocker = :me and b.blockee = u) or (b.blocker = u and b.blockee = :me))
            """)
    Page<FollowListResponse> findFollowers(
            @Param("me") User me,
            @Param("target") User target,
            @Param("state") FollowState state,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 차단 시 양방향 팔로우·팔로우 요청 삭제 (차단 해제 시 자동 복구하지 않음)
    @Modifying(clearAutomatically = true)
    @Query("delete from Follow f where (f.follower = :a and f.followee = :b) or (f.follower = :b and f.followee = :a)")
    void deleteAllBetween(@Param("a") User a, @Param("b") User b);
}
