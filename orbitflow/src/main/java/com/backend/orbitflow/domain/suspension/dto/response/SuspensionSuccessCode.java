package com.backend.orbitflow.domain.suspension.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuspensionSuccessCode implements SuccessCode {

    SUSPENSION_CREATE(HttpStatus.CREATED, "사용자가 정지되었습니다."),
    GET_SUSPENSION_LIST(HttpStatus.OK, "정지 리스트가 열람되었습니다."),
    GET_SUSPENSION_INFO(HttpStatus.OK, "정지 정보가 열람되었습니다."),
    SUSPENSION_UPDATE(HttpStatus.OK, "정지 정보가 수정되었습니다."),
    SUSPENSION_RELEASE(HttpStatus.OK, "정지가 해제되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
