package com.backend.orbitflow.domain.purge.scheduler;

import com.backend.orbitflow.domain.purge.service.DataPurgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 매일 04:30 : 논리적 삭제 후 30일이 지난 탈퇴 사용자·팀·투두 영구 삭제
// TODO: 다중 인스턴스 배포 시 ShedLock 등으로 중복 실행 방지
@Component
@RequiredArgsConstructor
public class DataPurgeScheduler {

    private final DataPurgeService dataPurgeService;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void purge() {
        dataPurgeService.purgeAll();
    }
}
