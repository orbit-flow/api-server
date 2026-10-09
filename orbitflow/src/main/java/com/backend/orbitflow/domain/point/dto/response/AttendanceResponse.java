package com.backend.orbitflow.domain.point.dto.response;

import com.backend.orbitflow.domain.avatar.dto.response.LevelResponse;

// 출석 결과 : 적립 포인트 거래 + 획득 경험치와 반영 후 레벨 (leveledUp : 이번 출석으로 레벨이 올랐는지)
public record AttendanceResponse(
        PointTransactionResponse transaction,
        int gainedExp,
        boolean leveledUp,
        LevelResponse level
) {
}
