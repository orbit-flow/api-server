package com.backend.orbitflow.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode{

    MAIL_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "메일 발송 중 오류가 발생했습니다.",
            "https://orbitflow.com/errors/mail-send-error",
            "Mail Send Error");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
