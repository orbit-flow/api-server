package com.backend.orbitflow.domain.block.repository;

import com.backend.orbitflow.domain.block.dto.response.BlockListResponse;
import com.backend.orbitflow.domain.block.entity.Block;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.Collection;

public interface BlockRepository extends JpaRepository<Block, Long> {

    Optional<Block> findByBlockerAndBlockee(User blocker, User blockee);

    // 차단은 양방향으로 적용되므로 a -> b, b -> a 중 하나라도 있으면 true
    @Query("select count(b) > 0 from Block b where (b.blocker = :a and b.blockee = :b) or (b.blocker = :b and b.blockee = :a)")
    boolean existsBetween(@Param("a") User a, @Param("b") User b);

    // blocker가 차단한 사용자 목록 (탈퇴 사용자 제외)
    @Query(value = """
            select new com.backend.orbitflow.domain.block.dto.response.BlockListResponse(
                b.id, u.uuid, u.name, u.profileImage, b.createdAt)
            from Block b
            join b.blockee u
            where b.blocker = :blocker
              and u.deletedAt is null
              and (:keyword is null or u.name like concat('%', :keyword, '%'))
            order by b.createdAt desc
            """,
            countQuery = """
            select count(b) from Block b
            join b.blockee u
            where b.blocker = :blocker
              and u.deletedAt is null
              and (:keyword is null or u.name like concat('%', :keyword, '%'))
            """)
    Page<BlockListResponse> findBlocks(
            @Param("blocker") User blocker,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 내가 차단한 사용자 (다수 참여 대화에서 차단한 사용자의 메시지 가림 표시용)
    @Query("select b.blockee.id from Block b where b.blocker = :blocker")
    Set<Long> findBlockeeIds(@Param("blocker") User blocker);

    // userIds 중 user와 어느 방향으로든 차단 관계인 사용자 id
    @Query("""
            select case when b.blocker = :user then b.blockee.id else b.blocker.id end
            from Block b
            where (b.blocker = :user and b.blockee.id in :userIds)
               or (b.blockee = :user and b.blocker.id in :userIds)
            """)
    Set<Long> findBlockedUserIdsAmong(@Param("user") User user, @Param("userIds") Collection<Long> userIds);

    // targetIds 중 누구든 allIds(targetIds 포함) 중 누구와 어느 방향으로든 차단 관계이면 true (다수 참여 대화 구성원 간 차단 검증용)
    @Query("""
            select count(b) > 0 from Block b
            where (b.blocker.id in :targetIds and b.blockee.id in :allIds)
               or (b.blockee.id in :targetIds and b.blocker.id in :allIds)
            """)
    boolean existsBlockAmong(@Param("targetIds") Collection<Long> targetIds, @Param("allIds") Collection<Long> allIds);
}
