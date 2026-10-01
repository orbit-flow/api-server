package com.backend.orbitflow.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
    @NotBlank(message = "사용자 이름은 비어있을 수 없습니다.")
    @Size(min = 2, max = 50, message = "사용자 이름은 2~50글자여야 합니다.")
    String name,

    @Size(max = 100, message = "자기소개는 100자 이내여야 합니다.")
    String introduce,

    String profileImage,
    
    boolean isPrivate
) {

}
