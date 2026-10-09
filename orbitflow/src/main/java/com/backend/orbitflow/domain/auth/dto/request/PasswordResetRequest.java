package com.backend.orbitflow.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 재설정 링크의 token으로 새 비밀번호 저장
public record PasswordResetRequest(
    @NotBlank(message = "재설정 토큰은 비어있을 수 없습니다.")
    String token,
    @NotBlank(message = "새 비밀번호는 비어있을 수 없습니다.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "비밀번호는 8~20글자여야 하며, 대소문자, 숫자, 특수문자를 포함해야 합니다."
    )
    String newPassword
) {
}
