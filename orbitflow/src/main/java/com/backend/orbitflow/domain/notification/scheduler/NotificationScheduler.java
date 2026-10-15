package com.backend.orbitflow.domain.notification.scheduler;

import com.backend.orbitflow.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import com.backend.orbitflow.global.util.RedisDistributedLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final RedisDistributedLock distributedLock;

    // 매일 04:00 : 생성 후 30일이 지난 알림 삭제
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredNotifications() {
        distributedLock.run("notification-expiry", Duration.ofMinutes(5), notificationService::deleteExpired);
    }
}
