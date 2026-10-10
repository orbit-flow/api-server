package com.backend.orbitflow.domain.suspension.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

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

    // 여러 계정을 한 번의 다중 키 삭제(DEL key...)로 해제 (일괄 만료 처리용)
    public void unmarkAll(Collection<String> uuids) {
        if (uuids.isEmpty()) {
            return;
        }
        redisTemplate.delete(uuids.stream().map(uuid -> KEY_PREFIX + uuid).toList());
    }

    public boolean isSuspended(String uuid) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + uuid));
    }

    // 로그아웃 토큰(블랙리스트 키는 access token 원문)과 정지 여부를 한 번의 Redis 왕복(MGET)으로 확인
    // 정지 표시는 항상 값("1")과 함께 저장되므로 값 존재 여부가 hasKey와 동일
    public AccessStatus checkAccess(String accessToken, String uuid) {
        List<String> values = redisTemplate.opsForValue().multiGet(List.of(accessToken, KEY_PREFIX + uuid));
        if (values == null) {
            return new AccessStatus(false, false);
        }
        return new AccessStatus(StringUtils.hasLength(values.get(0)), values.get(1) != null);
    }

    public record AccessStatus(boolean loggedOut, boolean suspended) {
    }
}
