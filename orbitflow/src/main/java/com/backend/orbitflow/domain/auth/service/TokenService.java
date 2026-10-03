package com.backend.orbitflow.domain.auth.service;

import com.backend.orbitflow.domain.user.enums.UserRole;
import org.springframework.http.ResponseCookie;

public interface TokenService {
    ResponseCookie createRefreshToken(String uuid);
    String getRefreshToken(String uuid);
    void deleteRefreshToken(String uuid);
    String getUuidFromRefreshToken(String refreshToken);
    String createAccessToken(String uuid, String email, UserRole role);
    String reissueToken(String uuid, String email, UserRole role);
    void addBlacklist(String accessToken);
}
