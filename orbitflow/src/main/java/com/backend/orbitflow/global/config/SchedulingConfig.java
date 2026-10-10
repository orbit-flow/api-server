package com.backend.orbitflow.global.config;

import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    // @Scheduled 전용 스케줄러
    // 지정하지 않으면 WebSocket 브로커의 messageBrokerTaskScheduler(스레드 수 = CPU 코어 수)를 함께 사용하므로 분리
    // @Scheduled는 TaskScheduler 빈이 여러 개면 이름이 taskScheduler인 빈을 사용
    // 매분 알림·리마인드, 30초 SSE heartbeat, 60초 STOMP 만료 점검이 새벽 배치(purge 등)에 밀리지 않도록 4개
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler(ThreadPoolTaskSchedulerBuilder builder) {
        return builder.poolSize(4)
                .threadNamePrefix("scheduling-")
                .build();
    }
}
