package com.backend.orbitflow.domain.user.scheduler;

import com.backend.orbitflow.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 매일 02:00 : 마지막 로그인으로부터 12개월이 지난 계정을 휴면(SLEEP) 전환
@Slf4j
@Component
@RequiredArgsConstructor
public class DormantUserScheduler {

    private final UserService userService;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void convertDormantUsers() {
        int converted = userService.convertDormantUsers();
        if (converted > 0) {
            log.info("휴면 계정 전환 완료: {}건", converted);
        }
    }
}
