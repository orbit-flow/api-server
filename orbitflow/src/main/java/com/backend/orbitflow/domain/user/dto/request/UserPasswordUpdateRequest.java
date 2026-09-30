package com.backend.orbitflow.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;

public record UserPasswordUpdateRequest(
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "비밀번호는 8~20글자여야 하며, 대소문자, 숫자, 특수문자를 포함해야 합니다."
    )
    String password,
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "비밀번호는 8~20글자여야 하며, 대소문자, 숫자, 특수문자를 포함해야 합니다."
    )
    String newPassword
) {
}
