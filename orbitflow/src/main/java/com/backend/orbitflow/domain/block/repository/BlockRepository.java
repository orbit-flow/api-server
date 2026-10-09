package com.backend.orbitflow.domain.block.repository;

import com.backend.orbitflow.domain.block.entity.Block;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlockRepository extends JpaRepository<Block, Long> {

    // 차단은 양방향으로 적용되므로 a -> b, b -> a 중 하나라도 있으면 true
    @Query("select count(b) > 0 from Block b where (b.blocker = :a and b.blockee = :b) or (b.blocker = :b and b.blockee = :a)")
    boolean existsBetween(@Param("a") User a, @Param("b") User b);
}
