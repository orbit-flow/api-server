package com.backend.orbitflow.domain.follow.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FollowSuccessCode implements SuccessCode {

    GET_FOLLOWING_LIST(HttpStatus.OK, "팔로잉 리스트가 열람되었습니다."),
    GET_FOLLOWER_LIST(HttpStatus.OK, "팔로워 리스트가 열람되었습니다."),
    GET_FOLLOW_REQUEST_LIST(HttpStatus.OK, "팔로우 요청 리스트가 열람되었습니다."),
    FOLLOW_TOGGLE_SUCCESS(HttpStatus.OK, "팔로우 토글이 완료되었습니다."),
    FOLLOW_ACCEPTED(HttpStatus.OK, "팔로우 요청이 승인되었습니다."),
    FOLLOW_DENIED(HttpStatus.OK, "팔로우가 삭제되었습니다."),
    FOLLOW_NOTIFICATION_TOGGLE(HttpStatus.OK, "팔로우 알림 설정이 변경되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
