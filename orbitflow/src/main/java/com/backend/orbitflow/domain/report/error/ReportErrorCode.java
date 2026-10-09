package com.backend.orbitflow.domain.report.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {

    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 신고입니다.",
            "https://orbitflow.com/errors/report-not-found",
            "Report Not Found"),
    REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND,
            "신고 대상이 존재하지 않습니다.",
            "https://orbitflow.com/errors/report-target-not-found",
            "Report Target Not Found"),
    INVALID_REPORT_TARGET(HttpStatus.BAD_REQUEST,
            "신고 대상이 올바르지 않습니다. (POST·COMMENT는 targetId, USER는 targetUserUuid 필요)",
            "https://orbitflow.com/errors/invalid-report-target",
            "Invalid Report Target"),
    UNSUPPORTED_REPORT_TYPE(HttpStatus.BAD_REQUEST,
            "아직 지원하지 않는 신고 유형입니다.",
            "https://orbitflow.com/errors/unsupported-report-type",
            "Unsupported Report Type"),
    SELF_REPORT(HttpStatus.BAD_REQUEST,
            "자기 자신 또는 자신의 콘텐츠는 신고할 수 없습니다.",
            "https://orbitflow.com/errors/self-report",
            "Self Report"),
    DUPLICATE_REPORT(HttpStatus.CONFLICT,
            "이미 처리 중인 신고가 있습니다.",
            "https://orbitflow.com/errors/duplicate-report",
            "Duplicate Report"),
    REPORT_TARGET_MISMATCH(HttpStatus.BAD_REQUEST,
            "신고의 피신고자와 정지 대상 사용자가 다릅니다.",
            "https://orbitflow.com/errors/report-target-mismatch",
            "Report Target Mismatch"),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST,
            "변경할 수 없는 신고 처리 상태입니다.",
            "https://orbitflow.com/errors/invalid-status-transition",
            "Invalid Status Transition");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
