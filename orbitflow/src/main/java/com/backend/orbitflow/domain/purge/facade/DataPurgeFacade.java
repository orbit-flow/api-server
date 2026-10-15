package com.backend.orbitflow.domain.purge.facade;

import com.backend.orbitflow.domain.purge.service.DataPurgeService;
import com.backend.orbitflow.domain.todo.service.TodoTransferService;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 탈퇴 사용자 한 명의 영구 삭제를 하나의 트랜잭션으로 조율 (DataPurgeScheduler가 사용자마다 호출)
@Component
@RequiredArgsConstructor
public class DataPurgeFacade {

    private final DataPurgeService dataPurgeService;
    private final TodoTransferService todoTransferService;

    // 락·재확인(복구된 사용자는 대상 아님) -> 팀 투두 상속 -> 나머지 삭제 순서
    @Transactional
    public void purgeUser(Long userId) {
        dataPurgeService.lockPurgeTarget(userId).ifPresent(this::purge);
    }

    // 담당 미완료 팀 투두는 팀 탈퇴와 같은 규칙으로 상속하고 상속받은 구성원에게 팀별로 알림
    private void purge(User user) {
        dataPurgeService.findJoinedTeams(user).forEach(team ->
                dataPurgeService.notifyInherited(team, todoTransferService.inherit(team, user)));
        dataPurgeService.purgeUserData(user);
    }
}
