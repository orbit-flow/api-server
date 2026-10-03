package com.backend.orbitflow.domain.auth.dto.response;

public record EmailVarifyResponse(
        boolean isVarify,
        String token
) {

    public static EmailVarifyResponse of(boolean isVarify, String token) {
        return new EmailVarifyResponse(isVarify, token);
    }
}
