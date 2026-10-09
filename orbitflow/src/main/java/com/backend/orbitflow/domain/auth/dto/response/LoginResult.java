package com.backend.orbitflow.domain.auth.dto.response;

import com.backend.orbitflow.domain.suspension.dto.response.SuspendedAccountResponse;

// 로그인 처리 결과 : 응답 본문 + 발급한 토큰 (정지 계정은 토큰 없음, 직렬화 대상 아님)
public record LoginResult(
        LoginResponse response,
        TokenResponse token
) {

    public static LoginResult success(TokenResponse token) {
        return new LoginResult(LoginResponse.active(), token);
    }

    public static LoginResult suspended(SuspendedAccountResponse suspension) {
        return new LoginResult(LoginResponse.suspended(suspension), null);
    }

    public boolean isSuspended() {
        return response.suspended();
    }
}
