package com.backend.orbitflow.domain.auth.dto.response;

public record EmailVerifyResponse(
        boolean isVerify,
        String token
) {

    public static EmailVerifyResponse of(boolean isVerify, String token) {
        return new EmailVerifyResponse(isVerify, token);
    }
}
