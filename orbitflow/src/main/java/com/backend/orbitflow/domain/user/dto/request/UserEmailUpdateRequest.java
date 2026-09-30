package com.backend.orbitflow.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserEmailUpdateRequest(
    @NotBlank(message = "이메일은 비어있을 수 없습니다.")
    @Email(message = "허용되지 않는 Email 입니다.")
    String email
) {

}
