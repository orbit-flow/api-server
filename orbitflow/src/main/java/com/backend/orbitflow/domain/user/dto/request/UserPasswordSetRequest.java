package com.backend.orbitflow.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 비밀번호가 없는 소셜 가입자의 최초 비밀번호 설정 (설정 후 가입 이메일·비밀번호로도 로그인 가능)
public record UserPasswordSetRequest(
    @NotBlank(message = "비밀번호는 비어있을 수 없습니다.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "비밀번호는 8~20글자여야 하며, 대소문자, 숫자, 특수문자를 포함해야 합니다."
    )
    String newPassword
) {
}
