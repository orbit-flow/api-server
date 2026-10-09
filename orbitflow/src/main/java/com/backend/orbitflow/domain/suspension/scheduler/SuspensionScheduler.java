package com.backend.orbitflow.domain.suspension.scheduler;

import com.backend.orbitflow.domain.suspension.service.SuspensionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 매일 00:05 만료된 기간 정지를 EXPIRED 처리
// 정지된 사용자 본인은 만료 즉시 로그인할 수 있음 (로그인 시 만료된 정지를 바로 해제), 배치는 남은 상태 정리용
@Component
@RequiredArgsConstructor
public class SuspensionScheduler {

    private final SuspensionService suspensionService;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void expireSuspensions() {
        suspensionService.expireAll();
    }
}
