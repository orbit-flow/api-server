package com.backend.orbitflow.domain.follow.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FollowErrorCode implements ErrorCode {

    SELF_FOLLOW(HttpStatus.BAD_REQUEST,
            "자기 자신을 팔로우할 수 없습니다.",
            "https://orbitflow.com/errors/self-follow",
            "Self Follow"),
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 팔로우입니다.",
            "https://orbitflow.com/errors/follow-not-found",
            "Follow Not Found"),
    ALREADY_ACCEPTED(HttpStatus.CONFLICT,
            "이미 승인된 팔로우입니다.",
            "https://orbitflow.com/errors/already-accepted",
            "Already Accepted"),
    NOT_FOLLOWING(HttpStatus.BAD_REQUEST,
            "팔로우 중인 사용자가 아닙니다.",
            "https://orbitflow.com/errors/not-following",
            "Not Following"),
    BLOCKED_USER(HttpStatus.FORBIDDEN,
            "차단 관계인 사용자와는 팔로우할 수 없습니다.",
            "https://orbitflow.com/errors/blocked-user",
            "Blocked User"),
    FOLLOW_LIST_FORBIDDEN(HttpStatus.FORBIDDEN,
            "팔로우 목록을 열람할 권한이 없습니다.",
            "https://orbitflow.com/errors/follow-list-forbidden",
            "Follow List Forbidden");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
