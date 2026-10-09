package com.backend.orbitflow.domain.auth.dto.response;

import com.backend.orbitflow.domain.suspension.dto.response.SuspendedAccountResponse;

// suspended가 true면 토큰을 발급하지 않고 정지 안내 정보만 반환 (FE는 정지 안내 화면만 표시)
public record LoginResponse(
        boolean suspended,
        // 정지 중일 때만 포함
        SuspendedAccountResponse suspension
) {

    public static LoginResponse active() {
        return new LoginResponse(false, null);
    }

    public static LoginResponse suspended(SuspendedAccountResponse suspension) {
        return new LoginResponse(true, suspension);
    }
}
