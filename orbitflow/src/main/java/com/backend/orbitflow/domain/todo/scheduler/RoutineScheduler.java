package com.backend.orbitflow.domain.todo.scheduler;

import com.backend.orbitflow.domain.todo.service.RoutineService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 매일 03:00(Asia/Seoul) 반복 회차를 1개월 앞까지 미리 생성
// TODO: 다중 인스턴스 배포 시 ShedLock 등으로 중복 실행 방지 (중복 생성은 (routine_id, start_date) 유니크 제약으로 차단됨)
@Component
@RequiredArgsConstructor
public class RoutineScheduler {

    private final RoutineService routineService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void generateRoutineOccurrences() {
        routineService.generateAll();
    }
}
