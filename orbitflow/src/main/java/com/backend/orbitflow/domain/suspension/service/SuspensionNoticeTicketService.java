package com.backend.orbitflow.domain.suspension.service;

import com.backend.orbitflow.domain.suspension.error.SuspensionErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

// 소셜 로그인으로 정지 계정에 로그인한 경우 FE 정지 안내 화면에 넘길 일회성 조회 키
// 세션(토큰)을 발급하지 않고도 안내 화면이 정지 사유·기간을 조회할 수 있도록 10분간 유지 (새로고침 대비 재조회 허용)
@Service
@RequiredArgsConstructor
public class SuspensionNoticeTicketService {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "suspension-notice:";

    private final RedisUtil redisUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    public String issue(String userUuid) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redisUtil.setValues(KEY_PREFIX + ticket, userUuid, TTL);
        return ticket;
    }

    public String getUserUuid(String ticket) {
        return redisUtil.getValues(KEY_PREFIX + ticket, String.class).orElseThrow(
                () -> new CommonException(SuspensionErrorCode.SUSPENSION_NOTICE_EXPIRED)
        );
    }
}
