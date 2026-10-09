package com.backend.orbitflow.domain.user.dto.response;

import com.backend.orbitflow.domain.user.entity.OAuthAccount;
import com.backend.orbitflow.domain.user.enums.Provider;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record OAuthAccountResponse(
        Provider provider,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime linkedAt
) {

    public static OAuthAccountResponse from(OAuthAccount account) {
        return new OAuthAccountResponse(account.getProvider(), account.getCreatedAt());
    }
}
