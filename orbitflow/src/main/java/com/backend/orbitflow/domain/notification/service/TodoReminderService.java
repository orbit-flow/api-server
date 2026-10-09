package com.backend.orbitflow.domain.notification.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// 투두 리마인드 : 투두 시작 시각 기준 remindBeforeMinutes 전에 담당자에게 발송 (투두당 1개)
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TodoReminderService {

    // TodoCreateRequest·TodoUpdateRequest의 remindBeforeMinutes 최대값과 동일
    private static final int MAX_REMIND_MINUTES = 10_080;

    private final TodoRepository todoRepository;
    private final NotificationService notificationService;

    // 직전 1분 구간 (windowEnd - 1분, windowEnd]에 리마인드 시각이 도래한 투두에 발송
    public void sendDueReminders(LocalDateTime now) {
        LocalDateTime windowEnd = now.truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime windowStart = windowEnd.minusMinutes(1);
        int sent = 0;
        for (Todo todo : todoRepository.findRemindCandidates(windowStart, windowEnd.plusMinutes(MAX_REMIND_MINUTES))) {
            LocalDateTime remindAt = todo.getStartDate().minusMinutes(todo.getRemindBeforeMinutes());
            if (!remindAt.isAfter(windowStart) || remindAt.isAfter(windowEnd) || !isActive(todo.getCategory())) {
                continue;
            }
            notificationService.send(todo.getAssignee(), NotificationType.REMINDER, null, todo.getId(), null, content(todo));
            sent++;
        }
        if (sent > 0) {
            log.info("투두 리마인드 발송 완료: {}건", sent);
        }
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
