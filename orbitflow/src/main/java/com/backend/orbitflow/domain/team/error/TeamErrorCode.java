package com.backend.orbitflow.domain.team.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TeamErrorCode implements ErrorCode {

    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 팀입니다.",
            "https://orbitflow.com/errors/team-not-found",
            "Team Not Found"),
    NOT_TEAM_OWNER(HttpStatus.FORBIDDEN,
            "팀 소유자만 수행할 수 있습니다.",
            "https://orbitflow.com/errors/not-team-owner",
            "Not Team Owner");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
