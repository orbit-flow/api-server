package com.backend.orbitflow.domain.follow.facade;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.follow.dto.response.FollowListResponse;
import com.backend.orbitflow.domain.follow.dto.response.FollowResponse;
import com.backend.orbitflow.domain.follow.entity.Follow;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.follow.error.FollowErrorCode;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowFacade {

    private final FollowService followService;
    private final UserService userService;
    private final BlockService blockService;

    public PageResponse<FollowListResponse> getFollowing(AuthUser authUser, String uuid, int page, int size, String keyword) {
        User me = userService.getByUuid(authUser.getUuid());
        User target = userService.getByUuid(uuid);
        validateFollowListAccess(me, target);
        return PageResponse.from(followService.getFollowings(me, target, page, size, keyword));
    }

    public PageResponse<FollowListResponse> getFollower(AuthUser authUser, String uuid, int page, int size, String keyword) {
        User me = userService.getByUuid(authUser.getUuid());
        User target = userService.getByUuid(uuid);
        validateFollowListAccess(me, target);
        return PageResponse.from(followService.getFollowers(me, target, page, size, keyword));
    }

    public PageResponse<FollowListResponse> getFollowRequests(AuthUser authUser, int page, int size, String keyword) {
        User me = userService.getByUuid(authUser.getUuid());
        return PageResponse.from(followService.getFollowRequests(me, page, size, keyword));
    }

    // 관계가 없으면 팔로우(비밀계정이면 요청), 있으면 언팔로우·요청 철회
    public FollowResponse toggleFollow(AuthUser authUser, String uuid) {
        if (authUser.getUuid().equals(uuid)) {
            throw new CommonException(FollowErrorCode.SELF_FOLLOW);
        }
        User me = userService.getByUuid(authUser.getUuid());
        User target = userService.getByUuid(uuid);
        if (blockService.isBlocked(me, target)) {
            throw new CommonException(FollowErrorCode.BLOCKED_USER);
        }
        Follow follow = followService.toggleFollow(me, target);
        if (follow == null) {
            return FollowResponse.notFollow(target.getUuid());
        }
        return FollowResponse.of(follow.getId(), target.getUuid(), follow.getState());
    }

    public FollowResponse acceptFollow(AuthUser authUser, Long followId) {
        User me = userService.getByUuid(authUser.getUuid());
        Follow follow = followService.acceptFollow(me, followId);
        return FollowResponse.of(follow.getId(), follow.getFollower().getUuid(), follow.getState());
    }

    // 받은 팔로우 요청 거절 또는 팔로워 삭제 : 상대방 입장에서 NOT_FOLLOW
    public FollowResponse deniedFollow(AuthUser authUser, Long followId) {
        User me = userService.getByUuid(authUser.getUuid());
        Follow follow = followService.deniedFollow(me, followId);
        return FollowResponse.notFollow(follow.getFollower().getUuid());
    }

    public FollowResponse toggleNotification(AuthUser authUser, String uuid) {
        User me = userService.getByUuid(authUser.getUuid());
        User target = userService.getByUuid(uuid);
        Follow follow = followService.toggleNotification(me, target);
        return FollowResponse.of(follow.getId(), target.getUuid(), follow.getState());
    }

    // 차단 관계는 공개 범위보다 우선, 비밀계정은 본인 또는 승인된 팔로워만 열람 가능
    private void validateFollowListAccess(User me, User target) {
        if (me.getId().equals(target.getId())) {
            return;
        }
        if (blockService.isBlocked(me, target)) {
            throw new CommonException(FollowErrorCode.FOLLOW_LIST_FORBIDDEN);
        }
        if (target.isPrivate() && followService.getFollowState(me, target) != FollowState.ACCEPTED) {
            throw new CommonException(FollowErrorCode.FOLLOW_LIST_FORBIDDEN);
        }
    }
}
