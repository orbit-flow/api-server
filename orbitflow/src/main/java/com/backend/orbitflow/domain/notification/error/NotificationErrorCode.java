package com.backend.orbitflow.domain.notification.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 알림입니다.",
            "https://orbitflow.com/errors/notification-not-found",
            "Notification Not Found");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
