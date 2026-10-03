package com.backend.orbitflow.domain.auth.dto.response;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record EmailCodeResponse(
        LocalDateTime expireTime
) {
    public static EmailCodeResponse of(Long duration) {
        return new EmailCodeResponse(
                LocalDateTime.now().plus(duration, ChronoUnit.MILLIS)
        );
    }
}
