package com.backend.orbitflow.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSignupRequest(

    @NotBlank(message = "이메일은 비어있을 수 없습니다.")
    @Email(message = "허용되지 않는 Email 입니다.")
    String email,
    
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "비밀번호는 8~20글자여야 하며, 대소문자, 숫자, 특수문자를 포함해야 합니다."
    )
    String password,

    @NotBlank(message = "사용자 이름은 비어있을 수 없습니다.")
    @Size(min = 2, max = 50, message = "사용자 이름은 2~50글자여야 합니다.")
    String name
) {
}
