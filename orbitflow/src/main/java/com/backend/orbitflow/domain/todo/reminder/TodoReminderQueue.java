package com.backend.orbitflow.domain.todo.reminder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 투두 리마인드 예약 목록 (Redis Sorted Set)
 *
 * <ul>
 *   <li>member = 투두 id, score = 리마인드 시각(epoch ms) : 투두 저장·수정 시 TodoReminderListener가 등록·갱신·제거</li>
 *   <li>매분 도래한 항목만 조회하므로 평소에는 DB 조회 없음</li>
 *   <li>조회와 ZREM을 Lua 스크립트로 원자 실행해 꺼낸 쪽만 가져가므로 서버가 여러 대여도 같은 리마인드를 한 번만 발송</li>
 *   <li>꺼낸 항목의 처리가 실패하면 호출 측이 requeue로 다시 예약 (발송 전 프로세스가 중단되는 경우는 서버 시작 시 재적재로 복구)</li>
 *   <li>Redis 반영은 트랜잭션 커밋 후 (롤백된 변경이 예약 목록에 남지 않도록)</li>
 *   <li>목록과 DB가 어긋나도 발송 직전에 DB 상태로 다시 확인하므로 잘못 발송되지 않음</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoReminderQueue {

    private static final String KEY = "todo-reminders";
    private static final int CLAIM_BATCH = 500;
    // 도래한 항목을 최대 ARGV[2]개 조회해 바로 제거하고 반환 (같은 인스턴스를 재사용해 EVALSHA로 실행)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final RedisScript<List<String>> CLAIM_SCRIPT = (RedisScript) RedisScript.of("""
            local members = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, tonumber(ARGV[2]))
            if #members > 0 then
                redis.call('ZREM', KEYS[1], unpack(members))
            end
            return members
            """, List.class);

    private final StringRedisTemplate redisTemplate;

    // remindAt이 null이면 예약 해제
    public void scheduleAfterCommit(Long todoId, LocalDateTime remindAt) {
        Runnable apply = () -> {
            if (remindAt == null) {
                redisTemplate.opsForZSet().remove(KEY, todoId.toString());
            } else {
                redisTemplate.opsForZSet().add(KEY, todoId.toString(), toScore(remindAt));
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            apply.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                apply.run();
            }
        });
    }

    // 서버 시작 시 DB 기준으로 다시 채움 (이미 있는 항목은 점수만 갱신)
    public void scheduleAll(Map<Long, LocalDateTime> remindAtByTodoId) {
        if (remindAtByTodoId.isEmpty()) {
            return;
        }
        Set<ZSetOperations.TypedTuple<String>> tuples = remindAtByTodoId.entrySet().stream()
                .map(entry -> ZSetOperations.TypedTuple.of(
                        entry.getKey().toString(), toScore(entry.getValue())))
                .collect(Collectors.toSet());
        redisTemplate.opsForZSet().add(KEY, tuples);
    }

    // 리마인드 시각이 now 이전인 항목을 목록에서 꺼내 반환 (조회와 제거를 스크립트 1회로 원자 실행하므로 꺼낸 항목은 이 호출만 가져감)
    public List<Long> claimDue(LocalDateTime now) {
        List<String> due = redisTemplate.execute(CLAIM_SCRIPT, List.of(KEY),
                String.valueOf((long) toScore(now)), String.valueOf(CLAIM_BATCH));
        if (due == null || due.isEmpty()) {
            return List.of();
        }
        return due.stream()
                .map(Long::valueOf)
                .toList();
    }

    // 꺼냈지만 처리에 실패한 항목을 at 시각으로 다시 예약 (그 사이 새로 예약된 시각이 있으면 덮어쓰지 않음)
    public void requeue(Collection<Long> todoIds, LocalDateTime at) {
        if (todoIds.isEmpty()) {
            return;
        }
        double score = toScore(at);
        Set<ZSetOperations.TypedTuple<String>> tuples = todoIds.stream()
                .map(todoId -> ZSetOperations.TypedTuple.of(todoId.toString(), score))
                .collect(Collectors.toSet());
        // 항목 수와 무관하게 ZADD NX 1회
        try {
            redisTemplate.opsForZSet().addIfAbsent(KEY, tuples);
        } catch (RuntimeException e) {
            log.error("투두 리마인드 재예약 실패 : 투두 id {}", todoIds, e);
        }
    }

    public void removeAll(Collection<Long> todoIds) {
        if (!todoIds.isEmpty()) {
            redisTemplate.opsForZSet().remove(KEY, todoIds.stream().map(String::valueOf).toArray());
        }
    }

    private static double toScore(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
