package com.backend.orbitflow.domain.follow.facade;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.follow.dto.response.FollowListResponse;
import com.backend.orbitflow.domain.follow.dto.response.FollowResponse;
import com.backend.orbitflow.domain.follow.entity.Follow;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.follow.error.FollowErrorCode;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FollowFacade {

    private final FollowService followService;
    private final UserService userService;
    private final BlockService blockService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<FollowListResponse> getFollowing(AuthUser authUser, String uuid, int page, int size, String keyword) {
        User me = userService.getByUuid(authUser.getUuid());
        User target = userService.getByUuid(uuid);
        validateFollowListAccess(me, target);
        return PageResponse.from(followService.getFollowings(me, target, page, size, keyword));
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowListResponse> getFollower(AuthUser authUser, String uuid, int page, int size, String keyword) {
        User me = userService.getByUuid(authUser.getUuid());
        User target = userService.getByUuid(uuid);
        validateFollowListAccess(me, target);
        return PageResponse.from(followService.getFollowers(me, target, page, size, keyword));
    }

    @Transactional(readOnly = true)
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
        // 팔로우·팔로우 요청 활동은 대상 사용자에게
        String content = follow.isPending()
                ? me.getName() + "님이 팔로우를 요청했습니다."
                : me.getName() + "님이 회원님을 팔로우하기 시작했습니다.";
        eventPublisher.publishEvent(NotificationRequest.to(target, NotificationType.SOCIAL, me, follow.getId(), null, content));
        return FollowResponse.of(follow.getId(), target.getUuid(), follow.getState());
    }

    public FollowResponse acceptFollow(AuthUser authUser, Long followId) {
        User me = userService.getByUuid(authUser.getUuid());
        Follow follow = followService.acceptFollow(me, followId);
        // 비밀계정 팔로우 요청이 승인되면 요청자에게 (거절은 고지하지 않음)
        eventPublisher.publishEvent(NotificationRequest.to(follow.getFollower(), NotificationType.SOCIAL, me, follow.getId(), null,
                me.getName() + "님이 팔로우 요청을 수락했습니다."));
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
