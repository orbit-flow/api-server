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
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import com.backend.orbitflow.global.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuthFacade{

    private final AuthService authService;
    private final TokenService tokenService;
    private final UserService userService;
    private final EmailService emailService;
    private final SuspensionService suspensionService;

    // 탈퇴 유예 계정은 복구, 정지 계정은 정지 사유·기간과 함께 거부, 휴면 계정은 이메일 인증 안내와 함께 거부
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userService.getByEmail(request.email());
        authService.authenticate(user, request.password());
        userService.restoreIfWithdrawn(user);
        suspensionService.validateNotSuspended(user);
        userService.validateNotDormant(user);
        userService.updateLastLoginAt(user);
        return TokenResponse.of(
                tokenService.createAccessToken(user.getUuid(), user.getEmail(), user.getRole()),
                tokenService.createRefreshToken(user.getUuid())
        );
    }

    public void logout(AuthUser authUser, String accessToken) {
        tokenService.deleteRefreshToken(authUser.getUuid());
        tokenService.addBlacklist(accessToken);
    }

    public TokenResponse reissueToken(String refreshToken) {
        String uuid = tokenService.getUuidFromRefreshToken(refreshToken);
        User user = userService.getByUuid(uuid);
        userService.validateNotDormant(user);
        return TokenResponse.of(
                tokenService.reissueToken(user.getUuid(), user.getEmail(), user.getRole()),
                tokenService.createRefreshToken(user.getUuid())
        );
    }

    // 휴면 해제 : /email/sendcode로 받은 인증 코드로 본인 확인 후 재활성화 (이후 다시 로그인)
    @Transactional
    public void releaseDormant(DormantReleaseRequest request) {
        User user = userService.getByEmail(request.email());
        if (user.getDeletedAt() != null) {
            throw new CommonException(AuthErrorCode.NOT_DORMANT_ACCOUNT);
        }
        authService.verifyCode(request.email(), request.code());
        userService.releaseDormant(user);
    }

    public EmailCodeResponse sendCode(EmailCodeRequest request) {
        Long expireTime = 300000L; // 5분
        String code = authService.createCode(request.email(), expireTime);
        emailService.sendCodeByEmail(request.email(), code);
        return EmailCodeResponse.of(expireTime);
    }

    public EmailVarifyResponse varifyEmail(EmailVarifyRequest request) {
        Long varifyExpireTime = 900000L; // 15분
        return EmailVarifyResponse.of(
                true,
                authService.varifyEmail(request.email(), request.code(), varifyExpireTime)
        );
    }
}
