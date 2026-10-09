package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.todo.dto.response.RoutineResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoPreviewResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.entity.Routine;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.DurationType;
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
import java.util.List;
import java.util.Set;

// 생성된 회차의 수정·삭제는 개별 투두 단위로 처리하며, 규칙 변경은 아직 생성되지 않은 회차에만 적용
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoutineServiceImpl implements RoutineService {

    private final RoutineRepository routineRepository;
    private final TodoRepository todoRepository;
    private final TodoAuthorityService todoAuthorityService;

    // 권한 검사는 호출 측(TodoService)에서 수행
    public RoutineResponse upsertRoutine(Todo todo, DurationType durationType, int duration, Integer daysOfWeek, LocalDateTime repeatEndDate) {
        if (todo.isChild()) {
            throw new CommonException(TodoErrorCode.CHILD_TODO_ROUTINE);
        }
        Integer days = durationType == DurationType.WEEK ? daysOfWeek : null;
        if (days != null && (days < 1 || days > 127)) {
            throw new CommonException(TodoErrorCode.INVALID_DAYS_OF_WEEK);
        }
        if (repeatEndDate != null && repeatEndDate.isBefore(todo.getStartDate())) {
            throw new CommonException(TodoErrorCode.INVALID_TODO_PERIOD);
        }

        Routine routine = todo.getRoutine();
        if (routine == null) {
            routine = routineRepository.save(Routine.of(todo, duration, durationType, days, repeatEndDate));
            todo.updateRoutine(routine);
        } else {
            routine.updateRule(duration, durationType, days, repeatEndDate);
        }
        generate(routine, routine.getTodo().getStartDate(), horizon());
        return RoutineResponse.from(routine);
    }

    // 반복 해제 : 이미 생성된 회차는 일반 투두로 유지
    public void deleteRoutine(Todo todo) {
        Routine routine = todo.getRoutine();
        if (routine == null) {
            throw new CommonException(TodoErrorCode.ROUTINE_NOT_FOUND);
        }
        todoRepository.detachRoutine(routine);
        routineRepository.deleteById(routine.getId());
    }

    // 미리보기 회차에 작업을 요청하면 해당 회차를 즉시 생성
    public TodoResponse createOccurrence(User actor, Long routineId, LocalDateTime startDate) {
        Routine routine = routineRepository.findWithTodoById(routineId)
                .filter(r -> !r.getTodo().isDeleted())
                .orElseThrow(() -> new CommonException(TodoErrorCode.ROUTINE_NOT_FOUND));
        todoAuthorityService.checkEdit(routine.getTodo().getCategory(), actor);
        if (!routine.isOccurrence(startDate)) {
            throw new CommonException(TodoErrorCode.NOT_ROUTINE_OCCURRENCE);
        }
        if (todoRepository.findByRoutineAndStartDate(routine, startDate).isPresent()) {
            throw new CommonException(TodoErrorCode.OCCURRENCE_ALREADY_EXISTS);
        }
        return TodoResponse.from(todoRepository.save(Todo.occurrence(routine.getTodo(), routine, startDate)));
    }

    // [from, to) 구간의 아직 생성되지 않은 회차
    @Transactional(readOnly = true)
    public List<TodoPreviewResponse> getPreviews(User user, LocalDateTime from, LocalDateTime to) {
        List<TodoPreviewResponse> previews = new ArrayList<>();
        for (Routine routine : routineRepository.findAllActiveByUser(user, from)) {
            List<LocalDateTime> occurrences = routine.occurrencesBetween(from, to.minusNanos(1));
            if (occurrences.isEmpty()) {
                continue;
            }
            Set<LocalDateTime> existing = todoRepository.findStartDatesByRoutine(routine);
            occurrences.stream()
                    .filter(start -> !existing.contains(start))
                    .forEach(start -> previews.add(TodoPreviewResponse.of(routine, start)));
        }
        return previews;
    }

    // 스케줄러에서 매일 호출 : 오늘부터 1개월 뒤까지의 회차 생성
    public void generateAll() {
        LocalDateTime from = LocalDateTime.now().toLocalDate().atStartOfDay();
        int created = 0;
        for (Routine routine : routineRepository.findAllActive(from)) {
            created += generate(routine, from, horizon());
        }
        log.info("반복 회차 생성 완료: {}건", created);
    }

    private int generate(Routine routine, LocalDateTime from, LocalDateTime to) {
        Set<LocalDateTime> existing = todoRepository.findStartDatesByRoutine(routine);
        List<Todo> occurrences = routine.occurrencesBetween(from, to).stream()
                .filter(start -> !existing.contains(start))
                .map(start -> Todo.occurrence(routine.getTodo(), routine, start))
                .toList();
        todoRepository.saveAll(occurrences);
        return occurrences.size();
    }

    private LocalDateTime horizon() {
        return LocalDateTime.now().plusMonths(GENERATION_MONTHS);
    }
}
