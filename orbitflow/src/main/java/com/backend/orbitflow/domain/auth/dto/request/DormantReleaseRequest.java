package com.backend.orbitflow.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 휴면 해제 : POST /api/auth/email/sendcode 로 받은 인증 코드
public record DormantReleaseRequest(
        @NotBlank(message = "이메일은 비어있을 수 없습니다.")
        @Email(message = "허용되지 않는 Email 입니다.")
        String email,

        @NotBlank(message = "인증 코드는 비어있을 수 없습니다.")
        String code
) {
}
