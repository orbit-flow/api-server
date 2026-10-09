package com.backend.orbitflow.domain.todo.repository;

import com.backend.orbitflow.domain.todo.entity.Routine;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // 원본 투두가 삭제되지 않았고 반복이 끝나지 않은 규칙
    @Query("""
            select r from Routine r
            join fetch r.todo t
            join fetch t.category
            join fetch t.assignee
            where t.deletedAt is null
              and (r.repeatEndDate is null or r.repeatEndDate >= :from)
            """)
    List<Routine> findAllActive(@Param("from") LocalDateTime from);

    // 개인 대시보드 미리보기용 : 내 개인 카테고리의 반복 규칙
    @Query("""
            select r from Routine r
            join fetch r.todo t
            join fetch t.category c
            where c.user = :user
              and t.deletedAt is null
              and (r.repeatEndDate is null or r.repeatEndDate >= :from)
            """)
    List<Routine> findAllActiveByUser(@Param("user") User user, @Param("from") LocalDateTime from);
}
