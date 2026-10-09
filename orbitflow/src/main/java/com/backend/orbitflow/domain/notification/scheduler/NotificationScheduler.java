package com.backend.orbitflow.domain.notification.scheduler;

import com.backend.orbitflow.domain.notification.service.NotificationService;
import com.backend.orbitflow.domain.notification.service.TodoReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// TODO: 다중 인스턴스 배포 시 ShedLock 등으로 중복 실행 방지 (리마인드 중복 발송 가능)
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final TodoReminderService todoReminderService;
    private final NotificationService notificationService;

    // 매분 0초 : 직전 1분 동안 리마인드 시각이 도래한 투두에 발송
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sendReminders() {
        todoReminderService.sendDueReminders(LocalDateTime.now());
    }

    // 매일 04:00 : 생성 후 30일이 지난 알림 삭제
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredNotifications() {
        notificationService.deleteExpired();
    }
}
