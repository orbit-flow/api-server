package com.backend.orbitflow.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVarifyRequest(

        @NotBlank(message = "이메일은 비어있을 수 없습니다.")
        @Email(message = "이메일을 입력해 주세요.")
        String email,

        @NotBlank(message = "인증 코드는 비어있을 수 없습니다.")
        @Pattern(regexp = "\\d{6}")
        String code
) {
    public EmailVarifyRequest {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
