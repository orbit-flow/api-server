package com.backend.orbitflow.domain.notification.reminder;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
 *   <li>항목은 ZREM에 성공한 쪽만 가져가므로 서버가 여러 대여도 같은 리마인드를 한 번만 발송</li>
 *   <li>Redis 반영은 트랜잭션 커밋 후 (롤백된 변경이 예약 목록에 남지 않도록)</li>
 *   <li>목록과 DB가 어긋나도 발송 직전에 DB 상태로 다시 확인하므로 잘못 발송되지 않음</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TodoReminderQueue {

    private static final String KEY = "todo-reminders";
    private static final int CLAIM_BATCH = 500;

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

    // 리마인드 시각이 now 이전인 항목을 목록에서 꺼내 반환 (꺼내는 데 성공한 항목만)
    public List<Long> claimDue(LocalDateTime now) {
        Set<String> due = redisTemplate.opsForZSet().rangeByScore(KEY, Double.NEGATIVE_INFINITY, toScore(now), 0, CLAIM_BATCH);
        if (due == null || due.isEmpty()) {
            return List.of();
        }
        List<Long> claimed = new ArrayList<>();
        for (String member : due) {
            Long removed = redisTemplate.opsForZSet().remove(KEY, member);
            if (removed != null && removed > 0) {
                claimed.add(Long.valueOf(member));
            }
        }
        return claimed;
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
