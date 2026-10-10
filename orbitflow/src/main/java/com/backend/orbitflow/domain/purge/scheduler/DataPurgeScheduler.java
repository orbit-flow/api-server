package com.backend.orbitflow.domain.purge.scheduler;

import com.backend.orbitflow.domain.purge.service.DataPurgeService;
import com.backend.orbitflow.domain.todo.service.TodoTransferService;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

// 매일 04:30 : 논리적 삭제 후 30일이 지난 탈퇴 사용자·팀·투두 영구 삭제
// 탈퇴 사용자는 사용자 단위 트랜잭션 안에서 락·재확인 -> 팀 투두 상속(TodoTransferService) -> 나머지 삭제 순으로 여기서 조율 (한 건의 실패가 전체를 막지 않음)
// TODO: 다중 인스턴스 배포 시 ShedLock 등으로 중복 실행 방지
@Slf4j
@Component
public class DataPurgeScheduler {

    private final DataPurgeService dataPurgeService;
    private final TodoTransferService todoTransferService;
    private final TransactionTemplate transactionTemplate;

    public DataPurgeScheduler(DataPurgeService dataPurgeService, TodoTransferService todoTransferService,
                              PlatformTransactionManager transactionManager) {
        this.dataPurgeService = dataPurgeService;
        this.todoTransferService = todoTransferService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void purge() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(DataPurgeService.RETENTION_DAYS);
        int teams = dataPurgeService.purgeTeams(threshold);
        int users = purgeWithdrawnUsers(threshold);
        int todos = dataPurgeService.purgeTodos(threshold);
        log.info("영구 삭제 완료 : 팀 {}건, 탈퇴 사용자 {}건, 투두 {}건", teams, users, todos);
    }

    private int purgeWithdrawnUsers(LocalDateTime threshold) {
        List<Long> userIds = dataPurgeService.findWithdrawnUserIds(threshold);
        int done = 0;
        for (Long userId : userIds == null ? List.<Long>of() : userIds) {
            try {
                transactionTemplate.executeWithoutResult(s -> purgeUser(userId));
                done++;
            } catch (RuntimeException e) {
                log.error("탈퇴 사용자 영구 삭제 실패 : id={}", userId, e);
            }
        }
        return done;
    }

    // 하나의 트랜잭션 : 대상이 아니면(복구 등) 아무것도 하지 않음
    private void purgeUser(Long userId) {
        User user = dataPurgeService.lockPurgeTarget(userId).orElse(null);
        if (user == null) {
            return;
        }
        // 담당 미완료 팀 투두는 팀 탈퇴와 같은 규칙으로 상속하고 상속받은 구성원에게 팀별로 알림
        dataPurgeService.findJoinedTeams(user).forEach(team ->
                dataPurgeService.notifyInherited(team, todoTransferService.inherit(team, user)));
        dataPurgeService.purgeUserData(user);
    }
}
