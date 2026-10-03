package com.backend.orbitflow.domain.auth.facade;

import com.backend.orbitflow.domain.auth.dto.request.EmailVarifyRequest;
import com.backend.orbitflow.domain.auth.dto.request.LoginRequest;
import com.backend.orbitflow.domain.auth.dto.request.EmailCodeRequest;
import com.backend.orbitflow.domain.auth.dto.response.EmailCodeResponse;
import com.backend.orbitflow.domain.auth.dto.response.EmailVarifyResponse;
import com.backend.orbitflow.domain.auth.dto.response.TokenResponse;
import com.backend.orbitflow.domain.auth.service.AuthService;
import com.backend.orbitflow.domain.auth.service.TokenService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import com.backend.orbitflow.global.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFacade{

    private final AuthService authService;
    private final TokenService tokenService;
    private final UserService userService;
    private final EmailService emailService;

    public TokenResponse login(LoginRequest request) {
        User user = userService.getByEmail(request.email());
        authService.authenticate(user, request.password());
        return TokenResponse.of(
                tokenService.createAccessToken(user.getUuid(), user.getEmail(), user.getRole()),
                tokenService.createRefreshToken(user.getUuid())
        );
    }

    public void logout(AuthUser authUser, String accessToken) {
        tokenService.deleteRefreshToken(authUser.getUuid());
        tokenService.addBlacklist(accessToken);
    }

    public TokenResponse reissueToken(String refreshToken) {;
        String uuid = tokenService.getUuidFromRefreshToken(refreshToken);
        User user = userService.getByUuid(uuid);
        return TokenResponse.of(
                tokenService.reissueToken(user.getUuid(), user.getEmail(), user.getRole()),
                tokenService.createRefreshToken(user.getUuid())
        );
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
