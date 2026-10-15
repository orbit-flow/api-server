package com.backend.orbitflow.domain.purge.scheduler;

import com.backend.orbitflow.domain.purge.facade.DataPurgeFacade;
import com.backend.orbitflow.domain.purge.service.DataPurgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import com.backend.orbitflow.global.util.RedisDistributedLock;
import java.time.Duration;

// 매일 04:30 : 논리적 삭제 후 30일이 지난 탈퇴 사용자·팀·투두 영구 삭제
// 대상 한 건(사용자·팀·투두 묶음)마다 별도 트랜잭션(DataPurgeFacade·DataPurgeService의 @Transactional)으로 호출하므로 한 건의 실패가 전체를 막지 않음
// TODO: 다중 인스턴스 배포 시 ShedLock 등으로 중복 실행 방지
@Slf4j
@Component
@RequiredArgsConstructor
public class DataPurgeScheduler {

    private static final int MAX_TODO_ROUNDS = 20;

    private final DataPurgeService dataPurgeService;
    private final DataPurgeFacade dataPurgeFacade;
    private final RedisDistributedLock distributedLock;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void purge() {
        distributedLock.run("data-purge", Duration.ofHours(1), () -> {
            LocalDateTime threshold = LocalDateTime.now().minusDays(DataPurgeService.RETENTION_DAYS);
            int teams = purgeEach(dataPurgeService.findPurgeTeamIds(threshold), "삭제된 팀", dataPurgeService::purgeTeam);
            int users = purgeEach(dataPurgeService.findWithdrawnUserIds(threshold), "탈퇴 사용자", dataPurgeFacade::purgeUser);
            int todos = purgeTodos(threshold);
            log.info("영구 삭제 완료 : 팀 {}건, 탈퇴 사용자 {}건, 투두 {}건", teams, users, todos);
        });
    }

    private int purgeEach(List<Long> ids, String label, Consumer<Long> purge) {
        int done = 0;
        for (Long id : ids) {
            try {
                purge.accept(id);
                done++;
            } catch (RuntimeException e) {
                log.error("{} 영구 삭제 실패 : id={}", label, id, e);
            }
        }
        return done;
    }

    // 하위 투두부터 여러 차례에 걸쳐 삭제하므로 더 지울 것이 없을 때까지(최대 MAX_TODO_ROUNDS회) 묶음 단위로 반복
    private int purgeTodos(LocalDateTime threshold) {
        int total = 0;
        for (int round = 0; round < MAX_TODO_ROUNDS; round++) {
            int deleted = dataPurgeService.purgeTodoBatch(threshold);
            if (deleted == 0) {
                break;
            }
            total += deleted;
        }
        return total;
    }
}
