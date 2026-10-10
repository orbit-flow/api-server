package com.backend.orbitflow.domain.todo.repository;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.todo.entity.Routine;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.backend.orbitflow.domain.todo.dto.RoutineSlot;
import com.backend.orbitflow.domain.todo.dto.response.DashboardTodoResponse;
import com.backend.orbitflow.domain.todo.enums.TodoType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @Query(value = """
            select t from Todo t
            join fetch t.assignee
            where t.category = :category
              and t.deletedAt is null
            order by t.startDate asc, t.sortOrder asc, t.id asc
            """,
            countQuery = "select count(t) from Todo t where t.category = :category and t.deletedAt is null")
    Page<Todo> findActiveByCategory(@Param("category") Category category, Pageable pageable);

    // 복구 가능한(삭제 후 30일 이내) 투두
    @Query(value = """
            select t from Todo t
            join fetch t.assignee
            where t.category = :category
              and t.deletedAt > :threshold
            order by t.deletedAt desc
            """,
            countQuery = "select count(t) from Todo t where t.category = :category and t.deletedAt > :threshold")
    Page<Todo> findRestorableByCategory(@Param("category") Category category, @Param("threshold") LocalDateTime threshold, Pageable pageable);

    // 개인 대시보드 일정 칸반 : 내 개인 카테고리의 일정 투두 중 [from, to) 구간과 겹치는 투두, 시작 시각순 (응답 항목으로 바로 조회)
    @Query(value = """
            select new com.backend.orbitflow.domain.todo.dto.response.DashboardTodoResponse(
                t.id, r.id, pt.id, t.type, t.name, t.startDate, t.endDate, t.isCompleted, t.completedAt,
                t.remindBeforeMinutes, t.sortOrder, c.id, c.name, c.color)
            from Todo t
            join t.category c
            left join t.routine r
            left join t.parentTodo pt
            where c.user = :user
              and t.type = :type
              and t.deletedAt is null
              and t.startDate < :to
              and t.endDate >= :from
            order by t.startDate asc, t.sortOrder asc, t.id asc
            """,
            countQuery = """
            select count(t) from Todo t
            join t.category c
            where c.user = :user
              and t.type = :type
              and t.deletedAt is null
              and t.startDate < :to
              and t.endDate >= :from
            """)
    Page<DashboardTodoResponse> findDashboardSchedules(
            @Param("user") User user,
            @Param("type") TodoType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // 개인 대시보드 백로그 칸반 : 카테고리 생성순 → 시작 시각순
    @Query(value = """
            select new com.backend.orbitflow.domain.todo.dto.response.DashboardTodoResponse(
                t.id, r.id, pt.id, t.type, t.name, t.startDate, t.endDate, t.isCompleted, t.completedAt,
                t.remindBeforeMinutes, t.sortOrder, c.id, c.name, c.color)
            from Todo t
            join t.category c
            left join t.routine r
            left join t.parentTodo pt
            where c.user = :user
              and t.type = :type
              and t.deletedAt is null
              and t.startDate < :to
              and t.endDate >= :from
            order by c.createdAt asc, c.id asc, t.startDate asc, t.sortOrder asc, t.id asc
            """,
            countQuery = """
            select count(t) from Todo t
            join t.category c
            where c.user = :user
              and t.type = :type
              and t.deletedAt is null
              and t.startDate < :to
              and t.endDate >= :from
            """)
    Page<DashboardTodoResponse> findDashboardBacklogs(
            @Param("user") User user,
            @Param("type") TodoType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
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
    // 소유자가 탈퇴했거나 팀이 삭제된 카테고리의 투두는 제외
    @Query("""
            select t.id as id, t.startDate as startDate, t.remindBeforeMinutes as remindBeforeMinutes
            from Todo t
            join t.category c
            left join c.team ct
            left join c.user cu
            where t.remindBeforeMinutes is not null
              and t.isCompleted = false
              and t.deletedAt is null
              and t.startDate > :from
              and (ct is null or ct.deletedAt is null)
              and (cu is null or cu.deletedAt is null)
            """)
    List<ReminderTarget> findUpcomingReminders(@Param("from") LocalDateTime from);

    interface ReminderTarget {
        Long getId();
        LocalDateTime getStartDate();
        Integer getRemindBeforeMinutes();
    }

    // 부모 투두와 함께 삭제된 자식 투두 중 복구 후 리마인드를 다시 예약해야 하는 항목 (복구 전에 조회)
    @Query("""
            select t.id as id, t.startDate as startDate, t.remindBeforeMinutes as remindBeforeMinutes
            from Todo t
            where t.parentTodo = :parent
              and t.deletedAt = :deletedAt
              and t.remindBeforeMinutes is not null
              and t.isCompleted = false
            """)
    List<ReminderTarget> findReminderTargetsByParentAndDeletedAt(@Param("parent") Todo parent, @Param("deletedAt") LocalDateTime deletedAt);

    List<Todo> findAllByParentTodoAndDeletedAtIsNullOrderBySortOrderAsc(Todo parentTodo);

    @Query("select coalesce(max(t.sortOrder), -1) from Todo t where t.parentTodo = :parent")
    int findMaxSortOrderByParent(@Param("parent") Todo parent);

    // 여러 반복 규칙의 [from, to] 구간에 이미 생성된 회차 슬롯 (규칙마다 조회하지 않음)
    // 슬롯 = 회차의 원래 시각 (옮긴 회차도 원래 슬롯 기준, 원본·기존 회차는 occurrenceDate가 없어 시작 시각)
    // 논리적 삭제된 회차 포함 : 삭제한 회차가 다시 미리보기·생성되지 않도록
    // 다른 회차를 옮겨 차지한 시각(start_date)도 생성된 것으로 봄 : (routine_id, start_date) 유니크 제약과 충돌하지 않도록
    @Query("""
            select new com.backend.orbitflow.domain.todo.dto.RoutineSlot(t.routine.id, coalesce(t.occurrenceDate, t.startDate), t.startDate)
            from Todo t
            where t.routine in :routines
              and (coalesce(t.occurrenceDate, t.startDate) between :from and :to
                   or t.startDate between :from and :to)
            """)
    List<RoutineSlot> findSlotsByRoutineIn(
            @Param("routines") Collection<Routine> routines,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            select count(t) > 0 from Todo t
            where t.routine = :routine
              and (coalesce(t.occurrenceDate, t.startDate) = :slot or t.startDate = :slot)
            """)
    boolean existsByRoutineAndSlot(@Param("routine") Routine routine, @Param("slot") LocalDateTime slot);

    // 반복 규칙 변경·해제 시 정리 대상 : 원본을 제외한 삭제되지 않은 회차 중 슬롯이 from 이후인 것 (슬롯순)
    @Query("""
            select t.id as id, coalesce(t.occurrenceDate, t.startDate) as slot, t.startDate as startDate, t.isCompleted as completed
            from Todo t
            where t.routine = :routine
              and t.id <> :originId
              and t.deletedAt is null
              and coalesce(t.occurrenceDate, t.startDate) >= :from
            order by coalesce(t.occurrenceDate, t.startDate) asc, t.id asc
            """)
    List<OccurrenceRow> findOccurrenceRows(
            @Param("routine") Routine routine,
            @Param("originId") Long originId,
            @Param("from") LocalDateTime from
    );

    interface OccurrenceRow {
        Long getId();
        LocalDateTime getSlot();
        LocalDateTime getStartDate();
        Boolean getCompleted();
    }

    // 반복 회차 일괄 논리적 삭제 (회차에 달린 자식 투두 포함, 같은 시각이라 함께 복구 가능)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Todo t set t.deletedAt = :deletedAt where (t.id in :ids or t.parentTodo.id in :ids) and t.deletedAt is null")
    void softDeleteOccurrences(@Param("ids") Collection<Long> ids, @Param("deletedAt") LocalDateTime deletedAt);

    // 반복을 오늘부터 변경할 때 새 규칙에 맞는 회차를 새 반복으로 이동
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Todo t set t.routine = :to where t.id in :ids")
    void moveToRoutine(@Param("ids") Collection<Long> ids, @Param("to") Routine to);

    // 부모 투두와 같은 시각으로 자식 투두 논리적 삭제 (함께 복구하기 위함)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Todo t set t.deletedAt = :deletedAt where t.parentTodo = :parent and t.deletedAt is null")
    void softDeleteChildren(@Param("parent") Todo parent, @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Todo t set t.deletedAt = null where t.parentTodo = :parent and t.deletedAt = :deletedAt")
    void restoreChildren(@Param("parent") Todo parent, @Param("deletedAt") LocalDateTime deletedAt);

    // 반복 해제 시 회차들을 일반 투두로 전환 (원래 시각도 제거)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Todo t set t.routine = null, t.occurrenceDate = null where t.routine = :routine")
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

    // 카테고리별 상속 대상 : 카테고리를 볼 수 있는 남은 구성원 중 권한이 가장 낮은 1명 (탈퇴 유예·정지 사용자 제외)
    // 권한 = 보유 역할 mask의 OR 합산 (소유자는 전체 권한 63), 비트 수 → mask 값 → 가입 순으로 낮은 순서
    @Query(nativeQuery = true, value = """
            select x.category_id as categoryId, x.user_id as heirId
            from (
                select c.id as category_id, m.user_id,
                       row_number() over (partition by c.id order by bit_count(k.mask), k.mask, m.created_at, m.id) as rn
                from categories c
                join team_members m on m.team_id = c.team_id
                join users u on u.id = m.user_id and u.deleted_at is null and u.status <> 'BANNED'
                join (select m2.id as member_id,
                             case when t2.owner_id = m2.user_id then 63 else coalesce(bit_or(r2.permissions_mask), 0) end as mask
                      from team_members m2
                      join teams t2 on t2.id = m2.team_id
                      left join team_member_roles mr2 on mr2.member_id = m2.id
                      left join team_roles r2 on r2.id = mr2.role_id
                      where m2.team_id = :teamId
                      group by m2.id, m2.user_id, t2.owner_id) k on k.member_id = m.id
                where c.id in (:categoryIds)
                  and m.user_id <> :leaverId
                  and (c.visibility <> 'PRIVATE'
                       or (k.mask & 8) <> 0
                       or exists (select 1 from category_permissions cp where cp.category_id = c.id and cp.member_id = m.id)
                       or exists (select 1 from category_permissions cp
                                  join team_member_roles vmr on vmr.role_id = cp.role_id
                                  where cp.category_id = c.id and vmr.member_id = m.id))
            ) x
            where x.rn = 1
            """)
    List<Heir> findHeirs(@Param("teamId") Long teamId, @Param("leaverId") Long leaverId, @Param("categoryIds") Collection<Long> categoryIds);

    interface Heir {
        Long getCategoryId();
        Long getHeirId();
    }

    // 팀을 떠나는 구성원의 미완료 투두를 상속 대상에게 (받는 사람마다 1회)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            update todos set assignee_id = :heirId
            where assignee_id = :leaverId
              and is_completed = 0
              and category_id in (:categoryIds)
            """)
    int reassignIncompleteTodos(
            @Param("categoryIds") Collection<Long> categoryIds,
            @Param("leaverId") Long leaverId,
            @Param("heirId") Long heirId
    );
}
