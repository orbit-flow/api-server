package com.backend.orbitflow.domain.auth.service;

import com.backend.orbitflow.domain.auth.error.AuthErrorCode;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService{

    private final PasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public void verifyPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new CommonException(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    public void authenticate(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CommonException(AuthErrorCode.LOGIN_FAIL);
        }
    }

    public String createCode(String email, Long time) {
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        redisUtil.setValues(email, code, Duration.ofMillis(time));
        return code;
    }

    @Override
    public String varifyEmail(String email, String code, Long expireTime) {
        String varifyCode = redisUtil.getValues(email, String.class).orElseThrow(
                () -> new CommonException(AuthErrorCode.NO_VARIFY_CODE)
        );

        if (!varifyCode.equals(code)) {
            throw new CommonException(AuthErrorCode.EMAIL_VARIFY_FAIL);
        }
        redisUtil.deleteValues(email);

        // 가입·이메일 변경 시 제출할 인증 토큰 (기존에는 빈 문자열을 반환해 가입이 불가능했음)
        String token = UUID.randomUUID().toString().replace("-", "");
        redisUtil.setValues(email, token, Duration.ofMillis(expireTime));
        return token;
    }

    // 일회성 이메일 인증 (휴면 해제 등) : 코드 검증 후 즉시 폐기
    public void verifyCode(String email, String code) {
        String sentCode = redisUtil.getValues(email, String.class).orElseThrow(
                () -> new CommonException(AuthErrorCode.NO_VARIFY_CODE)
        );
        if (!sentCode.equals(code)) {
            throw new CommonException(AuthErrorCode.EMAIL_VARIFY_FAIL);
        }
        redisUtil.deleteValues(email);
    }

}
