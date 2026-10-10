package com.backend.orbitflow.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    // 데이터 저장 (TTL 설정 포함)
    public void setValues(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    // 데이터 조회
    public <T> Optional<T> getValues(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(clazz.cast(value));
    }

    // 데이터 삭제
    public boolean deleteValues(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // 키가 없을 때만 저장 (SET NX), 저장했으면 true
    public boolean setIfAbsent(String key, Object value, Duration timeout) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout));
    }

    // 카운터 1 증가 (INCR), 처음 증가한 경우에만 TTL 설정 : 증가 후 값 반환
    public long increment(String key, Duration timeout) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, timeout);
        }
        return count == null ? 0L : count;
    }

    // 조회와 삭제를 원자적으로 수행 (GETDEL) : 일회용 토큰의 동시 사용 방지
    public <T> Optional<T> getAndDeleteValues(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(clazz.cast(value));
    }
}
