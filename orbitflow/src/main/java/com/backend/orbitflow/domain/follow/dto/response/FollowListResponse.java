package com.backend.orbitflow.domain.follow.dto.response;

import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// myState : 요청자 -> 목록 사용자 방향의 팔로우 상태 (관계가 없으면 레포지토리에서 NOT_FOLLOW로 매핑)
public record FollowListResponse(
        Long followId,
        String uuid,
        String name,
        String profileImage,
        FollowState myState,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime followAt
) {
}
