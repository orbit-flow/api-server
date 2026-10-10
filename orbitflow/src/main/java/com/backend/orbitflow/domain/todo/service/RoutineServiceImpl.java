package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.todo.dto.response.RoutineResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.entity.Routine;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.DurationType;
import com.backend.orbitflow.domain.todo.enums.RoutineScope;
import com.backend.orbitflow.domain.todo.error.TodoErrorCode;
import com.backend.orbitflow.domain.todo.repository.RoutineRepository;
import com.backend.orbitflow.domain.todo.repository.TodoRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.backend.orbitflow.domain.todo.dto.RoutineSlot;
import com.backend.orbitflow.domain.todo.dto.response.DashboardTodoResponse;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.backend.orbitflow.global.util.JdbcBulkInserter;
import com.backend.orbitflow.domain.todo.reminder.TodoReminderQueue;

// 생성된 회차의 수정·삭제는 개별 투두 단위로 처리하며, 규칙 변경은 범위(전체·오늘부터)에 따라 오늘 이후의 미완료·미이동 회차까지 정리
// 회차 슬롯 = 회차의 원래 시각 (Todo.occurrenceDate, 없으면 시작 시각) : 사용자가 옮긴 회차의 원래 슬롯은 다시 생성하지 않음
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoutineServiceImpl implements RoutineService {

    private final JdbcBulkInserter jdbcBulkInserter;
    private final TodoReminderQueue todoReminderQueue;
    private final RoutineRepository routineRepository;
    private final TodoRepository todoRepository;
    private final TodoAuthorityService todoAuthorityService;
    private final PlatformTransactionManager transactionManager;

    private static final int GENERATION_CHUNK_SIZE = 200;
    // 오늘부터 변경할 때 기준 회차를 찾는 탐색 기간
    private static final int NEXT_OCCURRENCE_YEARS = 10;

    // 권한 검사는 호출 측(TodoService)에서 수행
    // 이미 반복 중인 투두(원본·회차)는 scope 필수, 새로 설정할 때는 scope 무시
    public RoutineResponse upsertRoutine(Todo todo, DurationType durationType, int duration, Integer daysOfWeek, LocalDateTime repeatEndDate, RoutineScope scope) {
        if (todo.isChild()) {
            throw new CommonException(TodoErrorCode.CHILD_TODO_ROUTINE);
        }
        Integer days = durationType == DurationType.WEEK ? daysOfWeek : null;
        if (days != null && (days < 1 || days > 127)) {
            throw new CommonException(TodoErrorCode.INVALID_DAYS_OF_WEEK);
        }

        // 과거 회차는 생성하지 않음 : 원본 시작일이 과거여도 오늘부터 생성 (삭제·정리된 과거 회차가 되살아나지 않도록)
        LocalDateTime today = startOfToday();
        Routine routine = todo.getRoutine();
        if (routine == null) {
            if (repeatEndDate != null && repeatEndDate.isBefore(todo.getStartDate())) {
                throw new CommonException(TodoErrorCode.INVALID_TODO_PERIOD);
            }
            routine = routineRepository.save(Routine.of(todo, duration, durationType, days, repeatEndDate));
            todo.updateRoutine(routine);
            LocalDateTime start = todo.getStartDate();
            generate(routine, laterOf(start, today), horizon());
            return RoutineResponse.from(routine);
        }
        if (scope == null) {
            throw new CommonException(TodoErrorCode.ROUTINE_SCOPE_REQUIRED);
        }
        // 회차에서 호출될 수 있으므로 원본 투두(카테고리·담당자)와 함께 다시 조회 (원본이 삭제되어도 반복 규칙은 유지)
        routine = routineRepository.findWithTodoById(routine.getId())
                .orElseThrow(() -> new CommonException(TodoErrorCode.ROUTINE_NOT_FOUND));
        // 원본이 오늘 이후에 시작하면 지나간 회차가 없으므로 전체 적용과 같음
        if (scope == RoutineScope.FROM_TODAY && routine.getTodo().getStartDate().isBefore(today)) {
            RoutineResponse split = splitRoutine(routine, days, duration, durationType, repeatEndDate, today);
            if (split != null) {
                return split;
            }
        }
        return updateRoutine(routine, days, duration, durationType, repeatEndDate, today);
    }

    // 전체 적용 : 규칙을 그대로 변경하고 새 규칙에 맞지 않는 오늘 이후의 미완료·미이동 회차는 삭제, 부족한 회차는 생성 (과거 회차는 유지)
    private RoutineResponse updateRoutine(Routine routine, Integer days, int duration, DurationType durationType, LocalDateTime repeatEndDate, LocalDateTime today) {
        Todo origin = routine.getTodo();
        if (repeatEndDate != null && repeatEndDate.isBefore(origin.getStartDate())) {
            throw new CommonException(TodoErrorCode.INVALID_TODO_PERIOD);
        }
        routine.updateRule(duration, durationType, days, repeatEndDate);

        List<TodoRepository.OccurrenceRow> rows = todoRepository.findOccurrenceRows(routine, origin.getId(), today);
        if (!rows.isEmpty()) {
            // 이미 생성된 회차가 horizon 너머에 있을 수 있으므로 가장 늦은 슬롯까지 새 규칙의 회차를 계산
            Set<LocalDateTime> slots = new HashSet<>(routine.occurrencesBetween(today, laterOf(horizon(), rows.get(rows.size() - 1).getSlot())));
            List<Long> stale = rows.stream()
                    .filter(row -> isUntouched(row) && !slots.contains(row.getSlot()))
                    .map(TodoRepository.OccurrenceRow::getId)
                    .toList();
            if (!stale.isEmpty()) {
                todoRepository.softDeleteOccurrences(stale, LocalDateTime.now());
                routine = reload(routine);
            }
        }
        LocalDateTime start = routine.getTodo().getStartDate();
        generate(routine, laterOf(start, today), horizon());
        return RoutineResponse.from(routine);
    }

    // 오늘부터 적용 : 지나간 회차는 기존 반복에 남기고, 기준 회차(anchor)를 원본으로 하는 새 반복으로 분리
    // 기준 회차 : 오늘 이후 첫 회차(호출한 투두와 무관), 없으면 기존 규칙의 다음 회차를 생성
    // 기준 회차를 정할 수 없으면(더 이어질 회차가 없음) null
    private RoutineResponse splitRoutine(Routine routine, Integer days, int duration, DurationType durationType, LocalDateTime repeatEndDate, LocalDateTime today) {
        Todo origin = routine.getTodo();
        List<TodoRepository.OccurrenceRow> rows = todoRepository.findOccurrenceRows(routine, origin.getId(), today);

        // 새 반복의 기준은 시각을 옮기지 않은 첫 회차 (옮긴 회차를 기준으로 삼으면 새 규칙 전체가 옮긴 시각으로 밀림)
        TodoRepository.OccurrenceRow first = rows.stream()
                .filter(row -> row.getStartDate().equals(row.getSlot()))
                .findFirst()
                .orElse(null);
        LocalDateTime anchorStart;
        if (first != null) {
            anchorStart = first.getStartDate();
        } else {
            anchorStart = nextOccurrence(routine, today);
            if (anchorStart == null) {
                return null;
            }
        }
        if (repeatEndDate != null && repeatEndDate.isBefore(anchorStart)) {
            throw new CommonException(TodoErrorCode.INVALID_TODO_PERIOD);
        }
        Todo anchor = first == null
                ? todoRepository.save(Todo.occurrence(origin, routine, anchorStart))
                : todoRepository.findWithAllById(first.getId())
                        .orElseThrow(() -> new CommonException(TodoErrorCode.TODO_NOT_FOUND));

        // 기존 반복은 오늘 직전에 끝남 (이미 더 일찍 끝났다면 유지)
        LocalDateTime oldEnd = today.minusSeconds(1);
        LocalDateTime currentEnd = routine.getRepeatEndDate();
        routine.updateRule(routine.getDuration(), routine.getDurationType(), routine.getDaysOfWeek(),
                currentEnd != null && currentEnd.isBefore(oldEnd) ? currentEnd : oldEnd);
        Routine created = routineRepository.save(Routine.of(anchor, duration, durationType, days, repeatEndDate));
        anchor.promoteToOrigin(created);

        // 기존 반복의 오늘 이후 미완료·미이동 회차 : 새 규칙에 맞으면 새 반복으로 이동, 아니면 삭제 (옮겼거나 완료한 회차는 기존 반복에 유지)
        Long anchorId = anchor.getId();
        Set<LocalDateTime> slots = new HashSet<>(created.occurrencesBetween(today,
                rows.isEmpty() ? horizon() : laterOf(horizon(), rows.get(rows.size() - 1).getSlot())));
        List<Long> moved = new ArrayList<>();
        List<Long> stale = new ArrayList<>();
        for (TodoRepository.OccurrenceRow row : rows) {
            if (row.getId().equals(anchorId) || !isUntouched(row)) {
                continue;
            }
            (slots.contains(row.getSlot()) ? moved : stale).add(row.getId());
        }
        if (!moved.isEmpty()) {
            todoRepository.moveToRoutine(moved, created);
        }
        if (!stale.isEmpty()) {
            todoRepository.softDeleteOccurrences(stale, LocalDateTime.now());
        }
        // 일괄 변경으로 영속성 컨텍스트가 비워질 수 있으므로 새 반복을 다시 조회
        created = reload(created);
        LocalDateTime start = created.getTodo().getStartDate();
        generate(created, laterOf(start, today), horizon());
        return RoutineResponse.from(created);
    }

    // 반복 해제 : ALL은 이미 생성된 회차를 일반 투두로 유지, FROM_TODAY는 오늘부터 반복을 끝내고 오늘 이후의 미완료·미이동 회차 삭제
    public void deleteRoutine(Todo todo, RoutineScope scope) {
        Routine routine = todo.getRoutine();
        if (routine == null) {
            throw new CommonException(TodoErrorCode.ROUTINE_NOT_FOUND);
        }
        LocalDateTime today = startOfToday();
        Todo origin = routine.getTodo();
        // 원본이 오늘 이후에 시작하면 지나간 회차가 없으므로 전체 해제와 같음
        if (scope == RoutineScope.FROM_TODAY && origin.getStartDate().isBefore(today)) {
            LocalDateTime oldEnd = today.minusSeconds(1);
            LocalDateTime currentEnd = routine.getRepeatEndDate();
            routine.updateRule(routine.getDuration(), routine.getDurationType(), routine.getDaysOfWeek(),
                    currentEnd != null && currentEnd.isBefore(oldEnd) ? currentEnd : oldEnd);
            List<Long> stale = todoRepository.findOccurrenceRows(routine, origin.getId(), today).stream()
                    .filter(this::isUntouched)
                    .map(TodoRepository.OccurrenceRow::getId)
                    .toList();
            if (!stale.isEmpty()) {
                todoRepository.softDeleteOccurrences(stale, LocalDateTime.now());
            }
            return;
        }
        todoRepository.detachRoutine(routine);
        routineRepository.deleteById(routine.getId());
    }

    // 미리보기 회차에 작업을 요청하면 해당 회차를 즉시 생성
    public TodoResponse createOccurrence(User actor, Long routineId, LocalDateTime startDate) {
        Routine routine = routineRepository.findWithTodoById(routineId)
                .orElseThrow(() -> new CommonException(TodoErrorCode.ROUTINE_NOT_FOUND));
        todoAuthorityService.checkEdit(routine.getTodo().getCategory(), actor);
        // 생성 기준(1개월 뒤)을 넘거나 지나간(오늘 이전) 회차를 임의로 만들어 낼 수 없음
        if (!routine.isOccurrence(startDate) || startDate.isAfter(horizon()) || startDate.isBefore(startOfToday())) {
            throw new CommonException(TodoErrorCode.NOT_ROUTINE_OCCURRENCE);
        }
        if (todoRepository.existsByRoutineAndSlot(routine, startDate)) {
            throw new CommonException(TodoErrorCode.OCCURRENCE_ALREADY_EXISTS);
        }
        return TodoResponse.from(todoRepository.save(Todo.occurrence(routine.getTodo(), routine, startDate)));
    }

    // [from, to) 구간의 아직 생성되지 않은 회차 (반복 규칙 수와 무관하게 쿼리 2회 : 규칙, 생성된 회차 슬롯)
    // 과거 회차는 생성하지 않으므로 오늘 이전 구간은 미리보기에서도 제외
    @Transactional(readOnly = true)
    public List<DashboardTodoResponse> getPreviews(User user, LocalDateTime from, LocalDateTime to) {
        from = laterOf(from, startOfToday());
        if (!from.isBefore(to)) {
            return List.of();
        }
        LocalDateTime end = to.minusNanos(1);
        Map<Routine, List<LocalDateTime>> occurrencesByRoutine = new LinkedHashMap<>();
        for (Routine routine : routineRepository.findAllActiveByUser(user, from)) {
            List<LocalDateTime> occurrences = routine.occurrencesBetween(from, end);
            if (!occurrences.isEmpty()) {
                occurrencesByRoutine.put(routine, occurrences);
            }
        }
        if (occurrencesByRoutine.isEmpty()) {
            return List.of();
        }
        Map<Long, Set<LocalDateTime>> existing = existingSlots(occurrencesByRoutine.keySet(), from, end);
        List<DashboardTodoResponse> previews = new ArrayList<>();
        occurrencesByRoutine.forEach((routine, occurrences) -> occurrences.stream()
                .filter(start -> !existing.getOrDefault(routine.getId(), Set.of()).contains(start))
                .forEach(start -> previews.add(DashboardTodoResponse.preview(routine, start))));
        return previews;
    }

    // 스케줄러에서 매일 호출 : 오늘부터 1개월 뒤까지의 회차 생성
    // 반복 규칙을 묶음 단위로 나눠 묶음마다 별도 트랜잭션 (한 묶음의 실패가 전체를 막지 않고, 묶음당 조회 2회)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void generateAll() {
        LocalDateTime from = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime to = horizon();
        List<Long> routineIds = routineRepository.findAllActiveIds(from);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        int created = 0;
        for (int i = 0; i < routineIds.size(); i += GENERATION_CHUNK_SIZE) {
            List<Long> chunk = routineIds.subList(i, Math.min(i + GENERATION_CHUNK_SIZE, routineIds.size()));
            try {
                Integer count = transactionTemplate.execute(status ->
                        generate(routineRepository.findAllWithTodoByIdIn(chunk), from, to));
                created += count == null ? 0 : count;
            } catch (RuntimeException e) {
                log.error("반복 회차 생성 실패 : 규칙 id {} ~ {}", chunk.get(0), chunk.get(chunk.size() - 1), e);
            }
        }
        log.info("반복 회차 생성 완료: {}건", created);
    }

    private int generate(Routine routine, LocalDateTime from, LocalDateTime to) {
        return generate(List.of(routine), from, to);
    }

    // [from, to] 구간에서 아직 생성되지 않은 회차만 생성
    private int generate(List<Routine> routines, LocalDateTime from, LocalDateTime to) {
        if (routines.isEmpty()) {
            return 0;
        }
        Map<Long, Set<LocalDateTime>> existing = existingSlots(routines, from, to);
        List<Todo> occurrences = new ArrayList<>();
        for (Routine routine : routines) {
            Set<LocalDateTime> created = existing.getOrDefault(routine.getId(), Set.of());
            routine.occurrencesBetween(from, to).stream()
                    .filter(start -> !created.contains(start))
                    .forEach(start -> occurrences.add(Todo.occurrence(routine.getTodo(), routine, start)));
        }
        insertOccurrences(occurrences);
        return occurrences.size();
    }

    // 회차 수와 무관하게 INSERT 1회 (JDBC 배치), 엔티티 리스너를 거치지 않으므로 리마인드 예약은 직접 등록
    private void insertOccurrences(List<Todo> occurrences) {
        List<Long> ids = jdbcBulkInserter.insert("todos",
                List.of("category_id", "routine_id", "assignee_id", "type", "name", "start_date", "end_date",
                        "is_completed", "remind_before_minutes", "sort_order", "occurrence_date"),
                occurrences.stream()
                        .map(todo -> new Object[]{todo.getCategory().getId(), todo.getRoutine().getId(), todo.getAssignee().getId(),
                                todo.getType(), todo.getName(), todo.getStartDate(), todo.getEndDate(),
                                false, todo.getRemindBeforeMinutes(), todo.getSortOrder(), todo.getOccurrenceDate()})
                        .toList());
        for (int i = 0; i < occurrences.size(); i++) {
            LocalDateTime remindAt = occurrences.get(i).getRemindAt();
            if (remindAt != null) {
                todoReminderQueue.scheduleAfterCommit(ids.get(i), remindAt);
            }
        }
    }

    private Map<Long, Set<LocalDateTime>> existingSlots(Collection<Routine> routines, LocalDateTime from, LocalDateTime to) {
        return todoRepository.findSlotsByRoutineIn(routines, from, to).stream()
                .collect(Collectors.groupingBy(RoutineSlot::routineId,
                        Collectors.flatMapping(slot -> Stream.of(slot.slot(), slot.startDate()), Collectors.toSet())));
    }

    // 기존 규칙에서 from 이후 아직 생성되지 않은(삭제된 회차 제외) 첫 회차 시각, 없으면 null
    private LocalDateTime nextOccurrence(Routine routine, LocalDateTime from) {
        LocalDateTime to = from.plusYears(NEXT_OCCURRENCE_YEARS);
        Set<LocalDateTime> existing = existingSlots(List.of(routine), from, to).getOrDefault(routine.getId(), Set.of());
        return routine.occurrencesBetween(from, to).stream()
                .filter(start -> !existing.contains(start))
                .findFirst()
                .orElse(null);
    }

    // 사용자가 옮기지 않았고 완료하지 않은 회차 (규칙 변경 시 삭제·이동 대상)
    private boolean isUntouched(TodoRepository.OccurrenceRow row) {
        return !row.getCompleted() && row.getStartDate().equals(row.getSlot());
    }

    // 일괄 변경으로 영속성 컨텍스트가 비워진 뒤 원본 투두와 함께 다시 조회
    private Routine reload(Routine routine) {
        return routineRepository.findWithTodoById(routine.getId())
                .orElseThrow(() -> new CommonException(TodoErrorCode.ROUTINE_NOT_FOUND));
    }

    private LocalDateTime laterOf(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDateTime startOfToday() {
        return LocalDateTime.now().toLocalDate().atStartOfDay();
    }

    private LocalDateTime horizon() {
        return LocalDateTime.now().plusMonths(GENERATION_MONTHS);
    }
}
