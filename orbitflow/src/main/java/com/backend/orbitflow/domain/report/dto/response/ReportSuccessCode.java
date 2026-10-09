package com.backend.orbitflow.domain.report.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportSuccessCode implements SuccessCode {

    REPORT_CREATE(HttpStatus.CREATED, "신고가 접수되었습니다."),
    GET_REPORT_LIST(HttpStatus.OK, "신고 리스트가 열람되었습니다."),
    GET_REPORT_INFO(HttpStatus.OK, "신고 정보가 열람되었습니다."),
    REPORT_STATUS_UPDATE(HttpStatus.OK, "신고 처리 상태가 변경되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
