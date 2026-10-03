package com.backend.orbitflow.domain.auth.dto.response;

import org.springframework.http.ResponseCookie;

public record TokenResponse (
    String accessToken,
    ResponseCookie refreshToken
) {
    public static TokenResponse of(
            String accessToken, ResponseCookie refreshToken
    ) {
        return new TokenResponse(accessToken, refreshToken);
    }
}
