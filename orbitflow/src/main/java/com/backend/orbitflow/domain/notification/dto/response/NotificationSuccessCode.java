package com.backend.orbitflow.domain.notification.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationSuccessCode implements SuccessCode {

    GET_NOTIFICATION_LIST(HttpStatus.OK, "알림 리스트가 열람되었습니다."),
    GET_UNREAD_COUNT(HttpStatus.OK, "안 읽은 알림 수가 열람되었습니다."),
    NOTIFICATION_READ(HttpStatus.OK, "알림을 읽음 처리했습니다."),
    NOTIFICATION_READ_ALL(HttpStatus.OK, "모든 알림을 읽음 처리했습니다."),
    NOTIFICATION_DELETE(HttpStatus.OK, "알림이 삭제되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
