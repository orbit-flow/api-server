package com.backend.orbitflow.domain.suspension.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

// 정지 계정의 기존 세션(이미 발급된 access token) 차단용
// JwtAuthenticationFilter가 매 요청마다 DB 대신 Redis로 정지 여부 확인
@Component
@RequiredArgsConstructor
public class SuspendedUserCache {

    private static final String KEY_PREFIX = "suspended:";

    private final StringRedisTemplate redisTemplate;

    // 기간 정지는 만료 시각에 자동 삭제, 영구 정지는 해제 시까지 유지
    public void mark(String uuid, LocalDateTime expiresAt) {
        if (expiresAt == null) {
            redisTemplate.opsForValue().set(KEY_PREFIX + uuid, "1");
            return;
        }
        Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            unmark(uuid);
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + uuid, "1", ttl);
    }

    public void unmark(String uuid) {
        redisTemplate.delete(KEY_PREFIX + uuid);
    }

    public boolean isSuspended(String uuid) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + uuid));
    }
}
