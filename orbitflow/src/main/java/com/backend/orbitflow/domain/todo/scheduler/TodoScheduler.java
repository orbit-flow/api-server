package com.backend.orbitflow.domain.todo.scheduler;

import com.backend.orbitflow.domain.todo.facade.TodoFacade;
import com.backend.orbitflow.domain.todo.reminder.TodoReminderQueue;
import com.backend.orbitflow.domain.todo.service.RoutineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.time.Duration;
import com.backend.orbitflow.global.util.RedisDistributedLock;

// TODO: 다중 인스턴스 배포 시 ShedLock 등으로 중복 실행 방지 (중복 생성은 (routine_id, start_date) 유니크 제약으로 차단됨, 리마인드는 중복 발송 가능)
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoScheduler {

    private static final int GENERATION_CHUNK_SIZE = 200;

    private final RoutineService routineService;
    private final TodoFacade todoFacade;
    private final TodoReminderQueue todoReminderQueue;
    private final RedisDistributedLock distributedLock;

    // 매일 03:00(Asia/Seoul) 반복 회차를 1개월 앞까지 미리 생성
    // 반복 규칙을 묶음 단위로 나눠 묶음마다 별도 트랜잭션 (한 묶음의 실패가 전체를 막지 않음)
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void generateRoutineOccurrences() {
        distributedLock.run("todo-occurrence-generation", Duration.ofMinutes(30), () -> {
            List<Long> routineIds = routineService.findActiveRoutineIds();
            int created = 0;
            for (int i = 0; i < routineIds.size(); i += GENERATION_CHUNK_SIZE) {
                List<Long> chunk = routineIds.subList(i, Math.min(i + GENERATION_CHUNK_SIZE, routineIds.size()));
                try {
                    created += routineService.generateOccurrences(chunk);
                } catch (RuntimeException e) {
                    log.error("반복 회차 생성 실패 : 규칙 id {} ~ {}", chunk.get(0), chunk.get(chunk.size() - 1), e);
                }
            }
            log.info("반복 회차 생성 완료: {}건", created);
        });
    }

    // 매분 0초 : 직전 1분 동안 리마인드 시각이 도래한 투두에 발송
    // 꺼낸 항목의 처리 트랜잭션(TodoFacade.sendReminders)이 실패해도 재예약할 수 있도록 메서드 자체는 트랜잭션 없이 실행
    // 한 번에 꺼내는 수에 상한이 있으므로 같은 분에 몰린 리마인드는 목록이 빌 때까지 반복해서 처리
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sendReminders() {
        distributedLock.run("todo-reminders", Duration.ofSeconds(55), () -> {
            LocalDateTime now = LocalDateTime.now();
            while (true) {
                List<Long> todoIds = todoReminderQueue.claimDue(now);
                if (todoIds.isEmpty()) {
                    return;
                }
                try {
                    int sent = todoFacade.sendReminders(todoIds, now);
                    if (sent > 0) {
                        log.info("투두 리마인드 발송 완료: {}건", sent);
                    }
                } catch (RuntimeException e) {
                    log.error("투두 리마인드 발송 실패 : {}건 재예약", todoIds.size(), e);
                    todoReminderQueue.requeue(todoIds, now);
                    return;
                }
            }
        });
    }
}
