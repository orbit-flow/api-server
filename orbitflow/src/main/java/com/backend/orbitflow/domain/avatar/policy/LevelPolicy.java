package com.backend.orbitflow.domain.avatar.policy;

/**
 * 아바타 레벨·경험치 규칙
 *
 * <ul>
 *   <li>경험치는 출석으로만 획득 (유효한 출석 1회당 {@link #ATTENDANCE_EXP})</li>
 *   <li>avatars.exp는 누적 경험치, level은 누적 경험치로 계산한 값을 함께 저장</li>
 *   <li>레벨 L에서 L+1로 오르는 데 필요한 경험치 : 20 + 2L (레벨이 오를수록 필요한 출석 일수가 늘어남)
 *       - 1 → 2 : 22 (출석 3회), 50 → 51 : 120 (출석 12회), 98 → 99 : 216 (출석 22회)
 *       - 99레벨 도달에 누적 11,662 (매일 출석 시 약 3년 2개월)</li>
 *   <li>최대 레벨은 {@link #MAX_LEVEL}, 이후에도 누적 경험치는 계속 쌓임
 *       (최대 레벨 이후 적립분이 회수되어도 레벨이 잘못 내려가지 않도록 경험치를 상한으로 자르지 않음)</li>
 *   <li>무효·부정 출석 회수 시 해당 출석 경험치도 차감하고 레벨을 다시 계산 (레벨 하락 가능, 경험치는 0 미만이 되지 않음)</li>
 * </ul>
 */
public final class LevelPolicy {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 99;
    public static final int ATTENDANCE_EXP = 10;

    private LevelPolicy() {
    }

    // level에서 level + 1로 오르는 데 필요한 경험치
    public static long requiredExp(int level) {
        return 20L + 2L * level;
    }

    // level에 도달하기 위한 누적 경험치 : Σ(k = 1 .. level - 1) (20 + 2k) = (level - 1)(level + 20)
    public static long totalExpFor(int level) {
        long n = level - 1L;
        return n * (level + 20L);
    }

    public static int levelOf(long totalExp) {
        int level = MIN_LEVEL;
        while (level < MAX_LEVEL && totalExp >= totalExpFor(level + 1)) {
            level++;
        }
        return level;
    }
}
