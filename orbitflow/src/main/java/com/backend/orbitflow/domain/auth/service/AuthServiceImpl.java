package com.backend.orbitflow.domain.auth.service;

import com.backend.orbitflow.domain.auth.error.AuthErrorCode;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.error.UserErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.util.RedisUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * 이메일 OTP 인증
 *
 * <ul>
 *   <li>키는 용도별로 분리하고 이메일은 소문자로 통일 : code(OTP 5분), verified(인증 토큰 15분), cooldown(60초), count(시간당 발송 횟수), fail(검증 실패 횟수)</li>
 *   <li>발송은 60초에 1회, 1시간에 최대 5회</li>
 *   <li>OTP 검증 5회 실패 시 OTP 폐기 (재발송 필요), 성공 시 OTP를 폐기하고 난수 인증 토큰 발급</li>
 *   <li>가입·이메일 변경은 인증 토큰으로만 통과 (OTP 자체는 토큰으로 인정하지 않음)</li>
 * </ul>
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService{

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(15);
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration SEND_COUNT_TTL = Duration.ofHours(1);
    private static final int MAX_SEND_PER_HOUR = 5;
    private static final int MAX_VERIFY_FAILS = 5;

    private static final String CODE_KEY = "email:code:";
    private static final String VERIFIED_KEY = "email:verified:";
    private static final String COOLDOWN_KEY = "email:cooldown:";
    private static final String COUNT_KEY = "email:count:";
    private static final String FAIL_KEY = "email:fail:";

    private final PasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;
    private final SecureRandom secureRandom = new SecureRandom();
    // 존재하지 않는 계정 로그인 시에도 bcrypt 비용을 동일하게 쓰기 위한 더미 해시 (기동 시 1회 생성)
    private final String dummyPasswordHash;

    public AuthServiceImpl(PasswordEncoder passwordEncoder, RedisUtil redisUtil) {
        this.passwordEncoder = passwordEncoder;
        this.redisUtil = redisUtil;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public void verifyPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new CommonException(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    // DB 작업이 없으므로 트랜잭션 밖에서 호출되면 트랜잭션을 열지 않음 (bcrypt 동안 DB 커넥션 점유 방지)
    @Transactional(propagation = Propagation.SUPPORTS)
    public void authenticate(User user, String password) {
        // 비밀번호가 없는 소셜 전용 계정도 더미 해시와 비교해 응답 시간 차이를 줄임
        String encoded = user.getPassword() != null ? user.getPassword() : dummyPasswordHash;
        if (!passwordEncoder.matches(password, encoded) || user.getPassword() == null) {
            throw new CommonException(AuthErrorCode.LOGIN_FAIL);
        }
    }

    // 존재하지 않는 계정 : 더미 해시와 비교한 뒤 비밀번호 불일치와 동일한 실패 반환
    @Transactional(propagation = Propagation.SUPPORTS)
    public void failUnknownAccount(String password) {
        passwordEncoder.matches(password, dummyPasswordHash);
        throw new CommonException(AuthErrorCode.LOGIN_FAIL);
    }

    public long getCodeExpireMillis() {
        return CODE_TTL.toMillis();
    }

    // 발송 제한(60초 쿨다운, 시간당 5회)을 통과한 경우에만 OTP 저장
    public String createCode(String email) {
        if (!redisUtil.setIfAbsent(COOLDOWN_KEY + email, "1", SEND_COOLDOWN)) {
            throw new CommonException(AuthErrorCode.EMAIL_SEND_COOLDOWN);
        }
        if (redisUtil.increment(COUNT_KEY + email, SEND_COUNT_TTL) > MAX_SEND_PER_HOUR) {
            throw new CommonException(AuthErrorCode.EMAIL_SEND_LIMIT_EXCEEDED);
        }
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        redisUtil.setValues(CODE_KEY + email, code, CODE_TTL);
        // 새 OTP는 실패 횟수를 처음부터 다시 센다
        redisUtil.deleteValues(FAIL_KEY + email);
        return code;
    }

    @Override
    public String varifyEmail(String email, String code) {
        consumeCode(email, code);

        // 가입·이메일 변경 시 제출할 인증 토큰 (OTP와 별개의 난수)
        String token = UUID.randomUUID().toString().replace("-", "");
        redisUtil.setValues(VERIFIED_KEY + email, token, VERIFIED_TTL);
        return token;
    }

    // 일회성 이메일 인증 (휴면 해제 등) : 실패 횟수 제한을 동일하게 적용하고 성공 시 즉시 폐기
    public void verifyCode(String email, String code) {
        consumeCode(email, code);
    }

    // 가입·이메일 변경 : 인증 토큰 검증 후 1회용으로 폐기
    public void consumeVerifyToken(String email, String token) {
        String saved = redisUtil.getValues(VERIFIED_KEY + email, String.class).orElseThrow(
                () -> new CommonException(UserErrorCode.UN_VARIFIED_EMAIL)
        );
        if (token == null || !isEqual(saved, token) || !redisUtil.deleteValues(VERIFIED_KEY + email)) {
            throw new CommonException(UserErrorCode.UN_VARIFIED_EMAIL);
        }
    }

    // OTP 검증 : 비교 전에 실패 횟수를 먼저 올려 동시 요청으로 5회를 넘겨 시도하는 것을 방지
    private void consumeCode(String email, String code) {
        String saved = redisUtil.getValues(CODE_KEY + email, String.class).orElseThrow(
                () -> new CommonException(AuthErrorCode.NO_VARIFY_CODE)
        );
        long fails = redisUtil.increment(FAIL_KEY + email, CODE_TTL);
        if (fails > MAX_VERIFY_FAILS) {
            discardCode(email);
            throw new CommonException(AuthErrorCode.EMAIL_VARIFY_TOO_MANY_FAILS);
        }
        if (code == null || !isEqual(saved, code)) {
            if (fails >= MAX_VERIFY_FAILS) {
                discardCode(email);
                throw new CommonException(AuthErrorCode.EMAIL_VARIFY_TOO_MANY_FAILS);
            }
            throw new CommonException(AuthErrorCode.EMAIL_VARIFY_FAIL);
        }
        // 동시에 맞는 코드를 제출해도 한 번만 성공
        if (!redisUtil.deleteValues(CODE_KEY + email)) {
            throw new CommonException(AuthErrorCode.NO_VARIFY_CODE);
        }
        redisUtil.deleteValues(FAIL_KEY + email);
    }

    private void discardCode(String email) {
        redisUtil.deleteValues(CODE_KEY + email);
        redisUtil.deleteValues(FAIL_KEY + email);
    }

    private boolean isEqual(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

}
