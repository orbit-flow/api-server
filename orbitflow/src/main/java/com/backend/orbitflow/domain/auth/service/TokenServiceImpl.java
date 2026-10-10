package com.backend.orbitflow.domain.auth.service;

import com.backend.orbitflow.domain.auth.error.AuthErrorCode;
import com.backend.orbitflow.domain.user.enums.UserRole;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.security.JwtProvider;
import com.backend.orbitflow.global.util.RedisUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.backend.orbitflow.global.util.CookieUtil.createCookie;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService{

    private final JwtProvider jwtProvider;
    private final RedisUtil redisUtil;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenTime;

    @Override
    public ResponseCookie createRefreshToken(String uuid) {

        String refreshToken = jwtProvider.createRefreshToken(uuid);
        redisUtil.setValues(
                JwtProvider.REFRESH_HEADER + uuid,
                refreshToken,
                Duration.ofMillis(refreshTokenTime)
        );
        return createCookie(
                JwtProvider.REFRESH_HEADER,
                refreshToken,
                refreshTokenTime
        );
    }

    @Override
    public String getUuidFromRefreshToken(String refreshToken) {
        String uuid;
        try {
            uuid = jwtProvider.getUserInfoFromToken(refreshToken).getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw new CommonException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        // 조회와 삭제를 한 번에 수행해 같은 refresh token의 동시 재발급을 막음 (불일치여도 저장된 토큰은 폐기)
        String stored = redisUtil.getAndDeleteValues(JwtProvider.REFRESH_HEADER + uuid, String.class)
                .orElseThrow(() -> new CommonException(AuthErrorCode.INVALID_REFRESH_TOKEN));
        if (!refreshToken.equals(stored)) {
            throw new CommonException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        return uuid;
    }

    @Override
    public String getRefreshToken(String uuid) {
        return redisUtil.getValues(JwtProvider.REFRESH_HEADER + uuid, String.class)
                .orElseThrow(() -> new CommonException(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Override
    public void deleteRefreshToken(String uuid) {
        redisUtil.deleteValues(JwtProvider.REFRESH_HEADER + uuid);
    }

    @Override
    public String createAccessToken(String uuid, String email, UserRole role) {
        return jwtProvider.createAccessToken(uuid, email, role);
    }

    @Override
    public String reissueToken(String uuid, String email, UserRole role) {
        deleteRefreshToken(uuid);
        return createAccessToken(uuid, email, role);
    }

    public void addBlacklist(String accessToken) {
        try {
            long remainMs = jwtProvider.getUserInfoFromToken(accessToken)
                    .getExpiration().getTime() - System.currentTimeMillis();
            if (remainMs > 0) {
                redisUtil.setValues(accessToken, "logout", Duration.ofMillis(remainMs));
            }
        } catch (ExpiredJwtException e) {
            log.info("이미 만료된 Access Token이므로 블랙리스트 등록을 스킵합니다.");
        } catch (JwtException e) {
            log.warn("유효하지 않은 Access Token이므로 블랙리스트 등록을 스킵합니다.", e);
        }
    }
}
