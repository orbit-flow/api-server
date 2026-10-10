package com.backend.orbitflow.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "이메일은 비어있을 수 없습니다.")
        @Email(message = "허용되지 않는 Email 입니다.")
        String email,

        // 가입 정책이 바뀌어도 기존 사용자가 로그인할 수 있도록 형식 검증은 하지 않음
        @NotBlank(message = "비밀번호는 비어있을 수 없습니다.")
        String password
) {
    public LoginRequest {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
