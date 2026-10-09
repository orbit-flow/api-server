package com.backend.orbitflow.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 비밀번호 분실 : 재설정 링크 발송 요청
public record PasswordResetMailRequest(
        @NotBlank(message = "이메일은 비어있을 수 없습니다.")
        @Email(message = "이메일을 입력해 주세요.")
        String email
) {
}
