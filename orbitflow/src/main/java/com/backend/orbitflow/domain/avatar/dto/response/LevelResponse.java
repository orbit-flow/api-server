package com.backend.orbitflow.domain.avatar.dto.response;

import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.avatar.policy.LevelPolicy;

// totalExp : 누적 경험치, currentExp / requiredExp : 현재 레벨 구간의 진행도 (최대 레벨이면 둘 다 null)
public record LevelResponse(
        int level,
        int maxLevel,
        long totalExp,
        Long currentExp,
        Long requiredExp
) {

    public static LevelResponse from(Avatar avatar) {
        int level = avatar.getLevel();
        boolean max = level >= LevelPolicy.MAX_LEVEL;
        return new LevelResponse(
                level,
                LevelPolicy.MAX_LEVEL,
                avatar.getExp(),
                max ? null : avatar.getExp() - LevelPolicy.totalExpFor(level),
                max ? null : LevelPolicy.requiredExp(level)
        );
    }
}
