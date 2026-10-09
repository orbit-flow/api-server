package com.backend.orbitflow.domain.todo.repository;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.todo.entity.Routine;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.TodoType;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Query("""
            select t from Todo t
            join fetch t.category c
            left join fetch c.user
            left join fetch c.team
            join fetch t.assignee
            left join fetch t.parentTodo
            left join fetch t.routine
            where t.id = :id
            """)
    Optional<Todo> findWithAllById(@Param("id") Long id);

    @Query("""
            select t from Todo t
            join fetch t.assignee
            where t.category = :category
              and t.deletedAt is null
            order by t.startDate asc, t.sortOrder asc, t.id asc
            """)
    List<Todo> findActiveByCategory(@Param("category") Category category);

    // 복구 가능한(삭제 후 30일 이내) 투두
    @Query("""
            select t from Todo t
            join fetch t.assignee
            where t.category = :category
              and t.deletedAt > :threshold
            order by t.deletedAt desc
            """)
    List<Todo> findRestorableByCategory(@Param("category") Category category, @Param("threshold") LocalDateTime threshold);

    // 개인 대시보드 : 내 개인 카테고리의 투두 중 [from, to) 구간과 겹치는 투두
    @Query("""
            select t from Todo t
            join fetch t.category c
            join fetch t.assignee
            where c.user = :user
              and t.type = :type
              and t.deletedAt is null
              and t.startDate < :to
              and t.endDate >= :from
            order by c.createdAt asc, c.id asc, t.startDate asc, t.sortOrder asc, t.id asc
            """)
    List<Todo> findPersonalTodosBetween(
            @Param("user") User user,
            @Param("type") TodoType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    List<Todo> findAllByParentTodoAndDeletedAtIsNullOrderBySortOrderAsc(Todo parentTodo);

    @Query("select coalesce(max(t.sortOrder), -1) from Todo t where t.parentTodo = :parent")
    int findMaxSortOrderByParent(@Param("parent") Todo parent);

    // 논리적 삭제된 회차 포함
    @Query("select t.startDate from Todo t where t.routine = :routine")
    Set<LocalDateTime> findStartDatesByRoutine(@Param("routine") Routine routine);

    Optional<Todo> findByRoutineAndStartDate(Routine routine, LocalDateTime startDate);

    // 부모 투두와 같은 시각으로 자식 투두 논리적 삭제 (함께 복구하기 위함)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Todo t set t.deletedAt = :deletedAt where t.parentTodo = :parent and t.deletedAt is null")
    void softDeleteChildren(@Param("parent") Todo parent, @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Todo t set t.deletedAt = null where t.parentTodo = :parent and t.deletedAt = :deletedAt")
    void restoreChildren(@Param("parent") Todo parent, @Param("deletedAt") LocalDateTime deletedAt);

    // 반복 해제 시 회차들을 일반 투두로 전환
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Todo t set t.routine = null where t.routine = :routine")
    void detachRoutine(@Param("routine") Routine routine);

    // 카테고리 삭제 시 모든 소속 투두(논리적 삭제 포함) 이동
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Todo t set t.category = :to where t.category = :from")
    void moveAllToCategory(@Param("from") Category from, @Param("to") Category to);

    // 팀 탈퇴·추방 시 담당 미완료 팀 투두 상속
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Todo t set t.assignee = :newAssignee
            where t.assignee = :oldAssignee
              and t.isCompleted = false
              and t.category in (select c from Category c where c.team = :team)
            """)
    void reassignIncompleteTeamTodos(
            @Param("team") Team team,
            @Param("oldAssignee") User oldAssignee,
            @Param("newAssignee") User newAssignee
    );
}
