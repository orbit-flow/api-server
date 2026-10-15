package com.backend.orbitflow.domain.auth.service;

import com.backend.orbitflow.domain.user.enums.UserRole;
import org.springframework.http.ResponseCookie;

public interface TokenService {
    ResponseCookie createRefreshToken(String uuid);
    String getRefreshToken(String uuid);
    void deleteRefreshToken(String uuid);
    // 트랜잭션이 커밋된 뒤에 삭제 (롤백되면 삭제하지 않음), 트랜잭션 동기화가 없으면 바로 삭제
    void deleteRefreshTokenAfterCommit(String uuid);
    String getUuidFromRefreshToken(String refreshToken);
    String createAccessToken(String uuid, String email, UserRole role);
    String reissueToken(String uuid, String email, UserRole role);
    void addBlacklist(String accessToken);
}
