package com.backend.orbitflow.domain.auth.service;

import com.backend.orbitflow.domain.auth.error.AuthErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 비밀번호 재설정 링크 토큰
 *
 * <ul>
 *   <li>토큰은 32바이트 난수(URL-safe Base64)이며, Redis에는 SHA-256 해시만 저장 (Redis 유출 시에도 링크 재현 불가)</li>
 *   <li>유효 시간 30분, 1회용 : 사용 시 GETDEL로 원자적으로 폐기하여 동시 사용 불가</li>
 *   <li>사용자당 유효한 링크는 마지막 1개 : 새로 발급하면 이전 링크는 무효</li>
 *   <li>같은 이메일로는 1분에 1회만 요청 (가입 여부와 무관하게 동일하게 적용하여 계정 존재 여부를 노출하지 않음)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(1);
    private static final String TOKEN_KEY = "password-reset:token:";
    private static final String USER_KEY = "password-reset:user:";
    private static final String COOLDOWN_KEY = "password-reset:cooldown:";

    private final RedisUtil redisUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    public void checkCooldown(String email) {
        if (!redisUtil.setIfAbsent(COOLDOWN_KEY + email, "1", REQUEST_COOLDOWN)) {
            throw new CommonException(AuthErrorCode.PASSWORD_RESET_TOO_MANY_REQUESTS);
        }
    }

    public String issue(String userUuid) {
        redisUtil.getAndDeleteValues(USER_KEY + userUuid, String.class)
                .ifPresent(previousHash -> redisUtil.deleteValues(TOKEN_KEY + previousHash));

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = hash(token);
        redisUtil.setValues(TOKEN_KEY + hash, userUuid, TOKEN_TTL);
        redisUtil.setValues(USER_KEY + userUuid, hash, TOKEN_TTL);
        return token;
    }

    // 토큰을 폐기하고 대상 사용자 uuid 반환
    public String consume(String token) {
        String userUuid = redisUtil.getAndDeleteValues(TOKEN_KEY + hash(token), String.class).orElseThrow(
                () -> new CommonException(AuthErrorCode.INVALID_PASSWORD_RESET_TOKEN)
        );
        redisUtil.deleteValues(USER_KEY + userUuid);
        return userUuid;
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
