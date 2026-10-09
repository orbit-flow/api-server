package com.backend.orbitflow.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 새 이메일로 /api/auth/email/sendcode -> /api/auth/email/varify 후 받은 인증 토큰 필요
public record UserEmailUpdateRequest(
    @NotBlank(message = "이메일은 비어있을 수 없습니다.")
    @Email(message = "허용되지 않는 Email 입니다.")
    String email,

    @NotBlank(message = "이메일 인증 토큰을 입력해 주세요.")
    String emailVarifyToken
) {

}
