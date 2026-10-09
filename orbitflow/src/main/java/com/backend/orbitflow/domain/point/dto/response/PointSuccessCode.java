package com.backend.orbitflow.domain.point.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PointSuccessCode implements SuccessCode {

    ATTENDANCE_SUCCESS(HttpStatus.OK, "출석 포인트와 경험치가 적립되었습니다."),
    GET_ATTENDANCE_STATUS(HttpStatus.OK, "오늘 출석 여부가 열람되었습니다."),
    GET_POINT_HISTORY(HttpStatus.OK, "포인트 내역이 열람되었습니다."),
    POINT_REVOKE(HttpStatus.OK, "포인트가 회수되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
