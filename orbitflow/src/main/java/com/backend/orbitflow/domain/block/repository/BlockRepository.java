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
}
