package com.backend.orbitflow.domain.suspension.scheduler;

import com.backend.orbitflow.domain.suspension.service.SuspensionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 1분마다 만료된 기간 정지를 EXPIRED 처리 (로그인 시에도 즉시 확인하므로 지연은 최대 1분)
@Component
@RequiredArgsConstructor
public class SuspensionScheduler {

    private final SuspensionService suspensionService;

    @Scheduled(fixedDelay = 60_000)
    public void expireSuspensions() {
        suspensionService.expireAll();
    }
}
