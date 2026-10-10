package com.backend.orbitflow.domain.auth.facade;

import com.backend.orbitflow.domain.auth.dto.request.EmailVarifyRequest;
import com.backend.orbitflow.domain.auth.dto.request.DormantReleaseRequest;
import com.backend.orbitflow.domain.auth.dto.request.LoginRequest;
import com.backend.orbitflow.domain.auth.error.AuthErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.domain.auth.dto.request.EmailCodeRequest;
import com.backend.orbitflow.domain.auth.dto.response.EmailCodeResponse;
import com.backend.orbitflow.domain.auth.dto.response.EmailVarifyResponse;
import com.backend.orbitflow.domain.auth.dto.response.TokenResponse;
import com.backend.orbitflow.domain.auth.service.AuthService;
import com.backend.orbitflow.domain.auth.service.TokenService;
import com.backend.orbitflow.domain.suspension.service.SuspensionService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.UserStatus;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import com.backend.orbitflow.global.util.EmailService;
import com.backend.orbitflow.domain.auth.dto.request.PasswordResetMailRequest;
import com.backend.orbitflow.domain.auth.dto.request.PasswordResetRequest;
import com.backend.orbitflow.domain.auth.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.backend.orbitflow.domain.auth.dto.response.LoginResult;
import com.backend.orbitflow.domain.suspension.dto.response.SuspendedAccountResponse;
import com.backend.orbitflow.domain.suspension.error.SuspensionErrorCode;
import com.backend.orbitflow.domain.suspension.service.SuspensionNoticeTicketService;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthFacade{

    private final AuthService authService;
    private final TokenService tokenService;
    private final UserService userService;
    private final EmailService emailService;
    private final SuspensionService suspensionService;
    private final PasswordResetService passwordResetService;
    private final SuspensionNoticeTicketService suspensionNoticeTicketService;
    private final PlatformTransactionManager transactionManager;

    @Value("${app.front-url}")
    private String frontUrl;

    // 탈퇴 유예 계정은 복구, 휴면 계정은 이메일 인증 안내와 함께 거부
    // 정지 계정은 로그인은 성공하되 토큰 없이 정지 안내 정보만 반환 (정지 시 refresh token은 이미 폐기됨)
    // 비밀번호 검증(bcrypt)은 DB 커넥션을 잡지 않도록 트랜잭션 밖에서 먼저 수행하고, 상태 변경만 트랜잭션으로 처리
    public LoginResult login(LoginRequest request) {
        // 없는 계정과 비밀번호 불일치는 동일한 LOGIN_FAIL로 응답 (계정 존재 여부 비노출)
        User found = userService.findByEmail(request.email()).orElse(null);
        if (found == null) {
            authService.failUnknownAccount(request.password());
        }
        authService.authenticate(found, request.password());
        return new TransactionTemplate(transactionManager).execute(status -> completeLogin(request.email(), found));
    }

    // 검증 이후 계정이 바뀌었으면(이메일·비밀번호 변경) 동일한 LOGIN_FAIL로 처리
    private LoginResult completeLogin(String email, User verified) {
        User user = userService.findByEmail(email)
                .filter(current -> current.getId().equals(verified.getId())
                        && Objects.equals(current.getPassword(), verified.getPassword()))
                .orElseThrow(() -> new CommonException(AuthErrorCode.LOGIN_FAIL));
        userService.restoreIfWithdrawn(user);
        Optional<SuspendedAccountResponse> suspension = suspensionService.findActiveNotice(user);
        if (suspension.isPresent()) {
            return LoginResult.suspended(suspension.get());
        }
        userService.validateNotDormant(user);
        userService.updateLastLoginAt(user);
        return LoginResult.success(TokenResponse.of(
                tokenService.createAccessToken(user.getUuid(), user.getEmail(), user.getRole()),
                tokenService.createRefreshToken(user.getUuid())
        ));
    }

    // 소셜 로그인 정지 안내 : 실패 리다이렉트로 받은 조회 키로 정지 사유·기간 조회
    @Transactional
    public SuspendedAccountResponse getSuspensionNotice(String ticket) {
        User user = userService.getByUuidIncludingBanned(suspensionNoticeTicketService.getUserUuid(ticket));
        return suspensionService.findActiveNotice(user).orElseThrow(
                () -> new CommonException(SuspensionErrorCode.SUSPENSION_NOTICE_EXPIRED)
        );
    }

    public void logout(AuthUser authUser, String accessToken) {
        tokenService.deleteRefreshToken(authUser.getUuid());
        tokenService.addBlacklist(accessToken);
    }

    // 재발급도 접속으로 보아 마지막 접속 시각 갱신 (1시간마다 재발급되므로 하루 1회만 기록해 휴면 오판정 방지)
    @Transactional
    public TokenResponse reissueToken(String refreshToken) {
        String uuid = tokenService.getUuidFromRefreshToken(refreshToken);
        User user = userService.getByUuid(uuid);
        userService.validateNotDormant(user);
        if (user.getLastLoginAt() == null || user.getLastLoginAt().isBefore(LocalDateTime.now().minusDays(1))) {
            userService.updateLastLoginAt(user);
        }
        return TokenResponse.of(
                tokenService.reissueToken(user.getUuid(), user.getEmail(), user.getRole()),
                tokenService.createRefreshToken(user.getUuid())
        );
    }

    // 비밀번호 분실 : 가입된 이메일이면 재설정 링크 발송, 아니면 아무것도 하지 않음 (응답은 동일하게 성공)
    public void requestPasswordReset(PasswordResetMailRequest request) {
        passwordResetService.checkCooldown(request.email());
        userService.findByEmail(request.email()).ifPresent(user -> emailService.sendPasswordResetEmail(
                user.getEmail(),
                frontUrl + "/reset-password?token=" + passwordResetService.issue(user.getUuid())
        ));
    }

    // 링크 토큰으로 새 비밀번호 저장, 모든 기기 로그아웃(refresh token 폐기) 후 변경 안내 메일 발송
    // 소셜 전용 계정은 이 과정으로 비밀번호가 생기며 소셜 연결은 유지
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String uuid = passwordResetService.consume(request.token());
        User user = userService.resetPassword(uuid, authService.encodePassword(request.newPassword()));
        tokenService.deleteRefreshToken(uuid);
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getName());
    }

    // 휴면 해제 : /email/sendcode로 받은 인증 코드로 본인 확인 후 재활성화 (이후 다시 로그인)
    // 인증 코드를 먼저 검증하고(없는 계정에도 코드는 발송되므로 계정 여부와 무관), 계정 없음·휴면 아님·탈퇴 상태는 모두 동일한 실패로 응답
    @Transactional
    public void releaseDormant(DormantReleaseRequest request) {
        authService.verifyCode(request.email(), request.code());
        User user = userService.findByEmail(request.email())
                .filter(found -> found.getDeletedAt() == null && found.getStatus() == UserStatus.SLEEP)
                .orElseThrow(() -> new CommonException(AuthErrorCode.EMAIL_VARIFY_FAIL));
        userService.releaseDormant(user);
    }

    public EmailCodeResponse sendCode(EmailCodeRequest request) {
        String code = authService.createCode(request.email());
        emailService.sendCodeByEmail(request.email(), code);
        return EmailCodeResponse.of(authService.getCodeExpireMillis());
    }

    public EmailVarifyResponse varifyEmail(EmailVarifyRequest request) {
        return EmailVarifyResponse.of(
                true,
                authService.varifyEmail(request.email(), request.code())
        );
    }
}
