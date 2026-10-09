package com.backend.orbitflow.domain.todo.repository;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.todo.entity.Routine;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.TodoType;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
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

    // 타임라인 투두 완료 (kind = 1) : 정렬 키 (completedAt desc, kind asc, id desc)에서 커서 다음 항목
    @Query("""
            select t from Todo t
            join fetch t.assignee u
            join fetch t.category c
            left join fetch c.user
            left join fetch c.team
            where u in :users
              and u.deletedAt is null
              and t.isCompleted = true
              and t.completedAt is not null
              and t.deletedAt is null
              and (t.completedAt < :time
                   or (t.completedAt = :time and (:kind = 0 or (:kind = 1 and t.id < :id))))
            order by t.completedAt desc, t.id desc
            """)
    List<Todo> findTimelineCompletedTodos(
            @Param("users") Collection<User> users,
            @Param("time") LocalDateTime time,
            @Param("kind") int kind,
            @Param("id") Long id,
            Pageable pageable
    );

    // 리마인드 발송 대상 : 예약 목록(Redis)에서 꺼낸 투두를 발송 정보(담당자·카테고리 소유자·팀)와 함께 조회
    @Query("""
            select t from Todo t
            join fetch t.assignee
            join fetch t.category c
            left join fetch c.user
            left join fetch c.team
            where t.id in :ids
            """)
    List<Todo> findAllForReminderByIdIn(@Param("ids") Collection<Long> ids);

    // 서버 시작 시 리마인드 예약 목록 재적재용 : 아직 시작하지 않은, 리마인드가 설정된 미완료·미삭제 투두
    // 미리 생성된 반복 회차(최대 1개월 앞)까지 포함하며, 리마인드 시각이 지난 항목은 호출 측에서 제외
    @Query("""
            select t.id as id, t.startDate as startDate, t.remindBeforeMinutes as remindBeforeMinutes
            from Todo t
            where t.remindBeforeMinutes is not null
              and t.isCompleted = false
              and t.deletedAt is null
              and t.startDate > :from
            """)
    List<ReminderTarget> findUpcomingReminders(@Param("from") LocalDateTime from);

    interface ReminderTarget {
        Long getId();
        LocalDateTime getStartDate();
        Integer getRemindBeforeMinutes();
    }

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

    // 팀을 떠나는 구성원이 담당하던 미완료 팀 투두의 카테고리 (논리 삭제된 투두 포함, 복구 시 담당자가 필요하므로)
    @Query("""
            select distinct t.category from Todo t
            where t.assignee = :assignee
              and t.isCompleted = false
              and t.category.team = :team
            """)
    List<Category> findCategoriesWithIncompleteTodos(@Param("team") Team team, @Param("assignee") User assignee);

    // 팀을 떠나는 구성원의 미완료 투두 상속 (TeamTodoInheritanceService)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Todo t set t.assignee = :newAssignee
            where t.assignee = :oldAssignee
              and t.isCompleted = false
              and t.category = :category
            """)
    int reassignIncompleteTodos(
            @Param("category") Category category,
            @Param("oldAssignee") User oldAssignee,
            @Param("newAssignee") User newAssignee
    );
}
