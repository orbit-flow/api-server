package com.backend.orbitflow.domain.report.enums;

import java.util.Set;

// RECEIVED(접수) -> CONFIRMED(위반 확인) -> SANCTIONED(제재 완료), 처리 완료 전에는 REJECTED(기각) 가능
public enum ReportStatus {
    RECEIVED, CONFIRMED, SANCTIONED, REJECTED;

    // 처리 중인 상태 (중복 신고 판정용)
    public static final Set<ReportStatus> IN_PROGRESS = Set.of(RECEIVED, CONFIRMED);

    public boolean canTransitTo(ReportStatus next) {
        return switch (this) {
            case RECEIVED -> next == CONFIRMED || next == REJECTED;
            case CONFIRMED -> next == SANCTIONED || next == REJECTED;
            case SANCTIONED, REJECTED -> false;
        };
    }
}
