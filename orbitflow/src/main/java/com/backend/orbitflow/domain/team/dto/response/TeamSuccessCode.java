package com.backend.orbitflow.domain.team.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TeamSuccessCode implements SuccessCode {

    TEAM_CREATE(HttpStatus.CREATED, "팀이 생성되었습니다."),
    GET_TEAM_LIST(HttpStatus.OK, "팀 리스트가 열람되었습니다."),
    GET_TEAM_INFO(HttpStatus.OK, "팀 정보가 열람되었습니다."),
    TEAM_UPDATE(HttpStatus.OK, "팀 정보가 업데이트 되었습니다."),
    TEAM_DELETE(HttpStatus.OK, "팀이 삭제되었습니다."),
    GET_DELETED_TEAM_LIST(HttpStatus.OK, "복구 가능한 팀 리스트가 열람되었습니다."),
    TEAM_RESTORE(HttpStatus.OK, "팀이 복구되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
