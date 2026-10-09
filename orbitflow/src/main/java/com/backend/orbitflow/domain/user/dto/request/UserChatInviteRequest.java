package com.backend.orbitflow.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserChatInviteRequest(
    @NotNull(message = "대화 초대 허용 여부는 비어있을 수 없습니다.")
    Boolean allowNonFollowChatInvite
) {

}
