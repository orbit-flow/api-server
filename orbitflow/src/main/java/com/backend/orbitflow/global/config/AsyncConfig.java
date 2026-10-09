package com.backend.orbitflow.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// 알림 이벤트 처리(@Async @TransactionalEventListener)용, 실행기는 Spring Boot 기본 applicationTaskExecutor
@Configuration
@EnableAsync
public class AsyncConfig {

}
