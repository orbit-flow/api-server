package com.backend.orbitflow.domain.follow.dto.response;

import com.backend.orbitflow.domain.follow.enums.FollowState;

// uuid : 상대방 사용자 uuid, followId : 관계가 삭제된 경우 null
public record FollowResponse(
        Long followId,
        String uuid,
        FollowState state
) {

    public static FollowResponse of(Long followId, String uuid, FollowState state) {
        return new FollowResponse(
                followId, uuid, state
        );
    }

    public static FollowResponse notFollow(String uuid) {
        return new FollowResponse(
                null, uuid, FollowState.NOT_FOLLOW
        );
    }
}
