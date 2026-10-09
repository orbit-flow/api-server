package com.backend.orbitflow.domain.notification.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.reminder.TodoReminderQueue;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 투두 리마인드 : 투두 시작 시각 기준 remindBeforeMinutes 전에 담당자에게 발송 (투두당 1개)
// 발송 대상은 Redis 예약 목록(TodoReminderQueue)에서 꺼내므로 보낼 리마인드가 없으면 DB를 조회하지 않음
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TodoReminderService {

    // 서버 중단 등으로 늦어진 리마인드는 이 시간 안에서만 발송 (그보다 오래되면 의미가 없어 버림)
    private static final Duration LATE_TOLERANCE = Duration.ofMinutes(10);

    private final TodoRepository todoRepository;
    private final TodoReminderQueue todoReminderQueue;
    private final NotificationService notificationService;

    public void sendDueReminders(LocalDateTime now) {
        List<Long> todoIds = todoReminderQueue.claimDue(now);
        if (todoIds.isEmpty()) {
            return;
        }
        int sent = 0;
        for (Todo todo : todoRepository.findAllForReminderByIdIn(todoIds)) {
            // 예약 이후 변경(완료·삭제·시각 변경)이 반영되지 않은 항목일 수 있으므로 DB 기준으로 다시 확인
            LocalDateTime remindAt = todo.getRemindAt();
            if (remindAt == null || remindAt.isAfter(now)
                    || remindAt.isBefore(now.minus(LATE_TOLERANCE)) || !isActive(todo.getCategory())) {
                continue;
            }
            notificationService.send(todo.getAssignee(), NotificationType.REMINDER, null, todo.getId(), null, content(todo));
            sent++;
        }
        if (sent > 0) {
            log.info("투두 리마인드 발송 완료: {}건", sent);
        }
    }

    // 서버 시작 시 예약 목록 재적재 (Redis 데이터 유실 대비), 리마인드 시각이 아직 오지 않은 투두만
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void rebuildQueue() {
        LocalDateTime now = LocalDateTime.now();
        Map<Long, LocalDateTime> upcoming = new LinkedHashMap<>();
        for (TodoRepository.ReminderTarget target : todoRepository.findUpcomingReminders(now)) {
            LocalDateTime remindAt = target.getStartDate().minusMinutes(target.getRemindBeforeMinutes());
            if (remindAt.isAfter(now)) {
                upcoming.put(target.getId(), remindAt);
            }
        }
        todoReminderQueue.scheduleAll(upcoming);
        log.info("투두 리마인드 예약 목록 재적재: {}건", upcoming.size());
    }

    // 소유자가 탈퇴했거나 팀이 삭제된 카테고리의 투두는 제외
    private boolean isActive(Category category) {
        return category.isTeamCategory()
                ? category.getTeam().getDeletedAt() == null
                : category.getUser().getDeletedAt() == null;
    }

    private String content(Todo todo) {
        int minutes = todo.getRemindBeforeMinutes();
        return minutes == 0
                ? "'" + todo.getName() + "'이(가) 지금 시작됩니다."
                : "'" + todo.getName() + "' 시작 " + minutes + "분 전입니다.";
    }
}
