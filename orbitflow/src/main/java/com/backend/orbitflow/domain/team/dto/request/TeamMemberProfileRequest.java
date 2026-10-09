package com.backend.orbitflow.domain.team.dto.request;

import jakarta.validation.constraints.Size;

public record TeamMemberProfileRequest(
        // null이면 사용자 이름으로 표시
        @Size(max = 50, message = "팀 내 별칭은 50자 이내여야 합니다.")
        String nickname,

        @Size(max = 255, message = "팀 소개글은 255자 이내여야 합니다.")
        String bio
) {
}
