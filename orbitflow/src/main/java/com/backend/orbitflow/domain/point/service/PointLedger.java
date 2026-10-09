package com.backend.orbitflow.domain.point.service;

import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.avatar.repository.AvatarRepository;
import com.backend.orbitflow.domain.point.entity.PointTransaction;
import com.backend.orbitflow.domain.point.enums.PointTransactionType;
import com.backend.orbitflow.domain.point.error.PointErrorCode;
import com.backend.orbitflow.domain.point.repository.PointTransactionRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Connection;

/**
 * 포인트 원장 : 포인트 잔액(avatars.point) 변경의 유일한 진입점
 *
 * <p>동시성 규칙
 * <ul>
 *   <li>잔액 변경은 반드시 아바타 행 비관적 락(SELECT ... FOR UPDATE)을 잡은 트랜잭션 안에서만 수행</li>
 *   <li>잔액 변경과 거래 기록(balance_after)은 같은 트랜잭션에서 함께 반영되어, 실패 시 둘 다 반영되지 않음</li>
 *   <li>락 순서 : 다른 행(결제 등)과 함께 잠가야 하면 그 행을 먼저 잠그고 아바타를 마지막에 잠금 (데드락 방지)</li>
 *   <li>한 트랜잭션에서는 한 사용자의 아바타만 잠금</li>
 *   <li>락 대기 초과·데드락은 PessimisticLockingFailureException으로 전파되어 409로 응답</li>
 *   <li>격리 수준은 READ COMMITTED 필수 : MySQL 기본(REPEATABLE READ)에서는 락 획득 전 일반 조회 시점의 스냅샷이 고정되어,
 *       락을 얻은 뒤의 확인 조회(오늘 출석 여부, 이미 보유·회수 여부)가 앞선 요청의 커밋을 보지 못함</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PointLedger {

    private final AvatarRepository avatarRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final EntityManager entityManager;

    // 가입 시 기본 아바타와 초기 포인트 지급 (초기 포인트도 거래로 기록)
    // 새 행 생성이므로 다른 요청과 경합하지 않음
    @Transactional
    public Avatar createAvatar(User user) {
        return avatarRepository.findByUser(user).orElseGet(() -> {
            Avatar avatar = avatarRepository.save(Avatar.of(user));
            pointTransactionRepository.save(PointTransaction.of(
                    user, PointTransactionType.SIGNUP, Avatar.INITIAL_POINT, avatar.getPoint(), null
            ));
            return avatar;
        });
    }

    // 아바타 행 락 획득 후 최신 잔액으로 갱신 (이미 영속성 컨텍스트에 있던 오래된 상태를 사용하지 않음)
    // 같은 트랜잭션에서 다시 호출해도 이미 보유한 락이므로 대기하지 않음
    // 아바타 기능 이전 가입자는 최초 사용 시 생성 후 락
    public Avatar lock(User user) {
        requireReadCommitted();
        Avatar avatar = avatarRepository.findByUser(user).orElseGet(() -> createAvatar(user));
        entityManager.refresh(avatar, LockModeType.PESSIMISTIC_WRITE);
        return avatar;
    }

    // 잔액이 부족하면 거부 (구매·환불 등)
    public PointTransaction withdraw(User user, PointTransactionType type, int amount) {
        Avatar avatar = lock(user);
        if (!avatar.hasPoint(amount)) {
            throw new CommonException(PointErrorCode.NOT_ENOUGH_POINT);
        }
        return apply(avatar, user, type, -amount, null);
    }

    public PointTransaction deposit(User user, PointTransactionType type, int amount) {
        return apply(lock(user), user, type, amount, null);
    }

    // 무효·부정 출석 회수 : 잔액이 부족해도 회수하고 음수로 기록
    public PointTransaction revoke(User user, PointTransaction source) {
        return apply(lock(user), user, PointTransactionType.REVOKE, -source.getAmount(), source);
    }

    private PointTransaction apply(Avatar avatar, User user, PointTransactionType type, int amount, PointTransaction source) {
        avatar.addPoint(amount);
        return pointTransactionRepository.save(PointTransaction.of(user, type, amount, avatar.getPoint(), source));
    }

    // 포인트 변경 트랜잭션은 @Transactional(isolation = Isolation.READ_COMMITTED)로 시작해야 함
    private void requireReadCommitted() {
        Integer isolation = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        if (isolation == null || isolation != Connection.TRANSACTION_READ_COMMITTED) {
            throw new IllegalStateException("포인트 변경 트랜잭션은 READ COMMITTED 격리 수준이어야 합니다.");
        }
    }
}
