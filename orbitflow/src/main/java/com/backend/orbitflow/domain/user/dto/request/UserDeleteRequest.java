package com.backend.orbitflow.domain.user.dto.request;

// 이메일·비밀번호 가입자 : 현재 비밀번호 재입력 필수
// 비밀번호가 없는 소셜 가입자 : 사용자 이름 확인
public record UserDeleteRequest(
        String password,
        String confirmName
) {
}
