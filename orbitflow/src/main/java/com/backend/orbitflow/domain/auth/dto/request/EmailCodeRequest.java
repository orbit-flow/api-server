package com.backend.orbitflow.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailCodeRequest(

        @NotBlank(message = "이메일은 비어있을 수 없습니다.")
        @Email(message = "이메일을 입력해 주세요.")
        String email
){
    public EmailCodeRequest {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
