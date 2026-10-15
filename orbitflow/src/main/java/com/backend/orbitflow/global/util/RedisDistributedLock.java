package com.backend.orbitflow.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private final RedisUtil redisUtil;

    public boolean run(String name, Duration timeout, Runnable action) {
        String key = "scheduler:lock:" + name;
        String token = redisUtil.acquireLock(key, timeout);
        if (token == null) {
            return false;
        }
        try {
            action.run();
            return true;
        } finally {
            redisUtil.releaseLock(key, token);
        }
    }
}
