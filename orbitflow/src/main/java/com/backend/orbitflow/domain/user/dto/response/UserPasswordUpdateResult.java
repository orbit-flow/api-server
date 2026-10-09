package com.backend.orbitflow.domain.user.dto.response;

import org.springframework.http.ResponseCookie;

// 비밀번호 변경 결과 : 응답 본문 + 현재 기기에 새로 발급한 refresh token 쿠키 (직렬화 대상 아님)
public record UserPasswordUpdateResult(
        UserResponse user,
        ResponseCookie refreshToken
) {
}
