package com.backend.orbitflow.domain.todo.repository;

import com.backend.orbitflow.domain.todo.entity.Routine;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface RoutineRepository extends JpaRepository<Routine, Long> {

    @Query("""
            select r from Routine r
            join fetch r.todo t
            join fetch t.category c
            left join fetch c.user
            left join fetch c.team
            join fetch t.assignee
            where r.id = :id
            """)
    Optional<Routine> findWithTodoById(@Param("id") Long id);

    // 반복이 끝나지 않은 규칙 id (일일 회차 생성 배치를 묶음 단위로 나누기 위해 id만)
    // 원본 투두 삭제는 그 회차만 삭제한 것이므로 규칙은 유지, 소유자가 탈퇴했거나 팀이 삭제된 카테고리의 규칙은 제외
    @Query("""
            select r.id from Routine r
            join r.todo t
            join t.category c
            left join c.team ct
            left join c.user cu
            where (r.repeatEndDate is null or r.repeatEndDate >= :from)
              and (ct is null or ct.deletedAt is null)
              and (cu is null or cu.deletedAt is null)
            order by r.id
            """)
    List<Long> findAllActiveIds(@Param("from") LocalDateTime from);

    // 회차 생성에 필요한 원본 투두(카테고리·담당자)와 함께 조회
    @Query("""
            select r from Routine r
            join fetch r.todo t
            join fetch t.category
            join fetch t.assignee
            where r.id in :ids
            """)
    List<Routine> findAllWithTodoByIdIn(@Param("ids") Collection<Long> ids);

    // 개인 대시보드 미리보기용 : 내 개인 카테고리의 반복 규칙
    @Query("""
            select r from Routine r
            join fetch r.todo t
            join fetch t.category c
            where c.user = :user
              and (r.repeatEndDate is null or r.repeatEndDate >= :from)
            """)
    List<Routine> findAllActiveByUser(@Param("user") User user, @Param("from") LocalDateTime from);
}
