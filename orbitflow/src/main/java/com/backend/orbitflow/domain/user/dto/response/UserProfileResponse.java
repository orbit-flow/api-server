package com.backend.orbitflow.domain.user.dto.response;

import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.user.entity.User;

// 다른 사용자 프로필 열람용 : 이메일·계정 상태·설정값은 노출하지 않음 (내 정보는 UserResponse)
public record UserProfileResponse(

    String uuid,
    String name,
    String introduce,
    String profileImage,
    // 내가 이 사용자를 팔로우하는 상태 (NOT_FOLLOW / PENDING / ACCEPTED)
    FollowState followState,
    // 이 사용자가 나를 팔로우하는 상태 (NOT_FOLLOW / PENDING / ACCEPTED)
    FollowState followerState
) {

    public static UserProfileResponse from(User target, FollowState followState, FollowState followerState) {
        return new UserProfileResponse(
            target.getUuid(),
            target.getName(),
            target.getIntroduce(),
            target.getProfileImage(),
            followState,
            followerState
        );
    }
}
