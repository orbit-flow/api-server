package com.backend.orbitflow.domain.follow.service;

import com.backend.orbitflow.domain.follow.dto.response.FollowListResponse;
import com.backend.orbitflow.domain.follow.entity.Follow;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.follow.error.FollowErrorCode;
import com.backend.orbitflow.domain.follow.repository.FollowRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;

    // follows row가 없으면 NOT_FOLLOW
    @Transactional(readOnly = true)
    public FollowState getFollowState(User follower, User followee) {
        return followRepository.findByFollowerAndFollowee(follower, followee)
                .map(Follow::getState)
                .orElse(FollowState.NOT_FOLLOW);
    }

    @Transactional(readOnly = true)
    public Page<FollowListResponse> getFollowings(User me, User target, int page, int size, String keyword) {
        return followRepository.findFollowings(me, target, keyword, toPageable(page, size));
    }

    @Transactional(readOnly = true)
    public Page<FollowListResponse> getFollowers(User me, User target, int page, int size, String keyword) {
        return followRepository.findFollowers(me, target, FollowState.ACCEPTED, keyword, toPageable(page, size));
    }

    @Transactional(readOnly = true)
    public Page<FollowListResponse> getFollowRequests(User me, int page, int size, String keyword) {
        return followRepository.findFollowers(me, me, FollowState.PENDING, keyword, toPageable(page, size));
    }

    // 관계가 있으면 삭제(언팔로우·요청 철회) 후 null 반환, 없으면 생성
    public Follow toggleFollow(User follower, User followee) {
        Optional<Follow> follow = followRepository.findByFollowerAndFollowee(follower, followee);
        if (follow.isPresent()) {
            followRepository.delete(follow.get());
            return null;
        }
        return followRepository.save(Follow.of(followee, follower));
    }

    public Follow acceptFollow(User me, Long followId) {
        Follow follow = getReceivedFollow(me, followId);
        if (!follow.isPending()) {
            throw new CommonException(FollowErrorCode.ALREADY_ACCEPTED);
        }
        follow.acceptFollow();
        return follow;
    }

    // 받은 팔로우 요청 거절 또는 팔로워 삭제
    public Follow deniedFollow(User me, Long followId) {
        Follow follow = getReceivedFollow(me, followId);
        followRepository.delete(follow);
        return follow;
    }

    public Follow toggleNotification(User follower, User followee) {
        Follow follow = followRepository.findByFollowerAndFollowee(follower, followee)
                .filter(f -> f.getState() == FollowState.ACCEPTED)
                .orElseThrow(() -> new CommonException(FollowErrorCode.NOT_FOLLOWING));
        follow.toggleNotification();
        return follow;
    }

    public void deleteAllBetween(User a, User b) {
        followRepository.deleteAllBetween(a, b);
    }

    // 다른 사용자가 받은 팔로우 id로 접근하면 존재 여부를 노출하지 않도록 NOT_FOUND 처리
    private Follow getReceivedFollow(User me, Long followId) {
        Follow follow = followRepository.findWithUsersById(followId).orElseThrow(
                () -> new CommonException(FollowErrorCode.FOLLOW_NOT_FOUND)
        );
        if (!follow.getFollowee().getId().equals(me.getId())) {
            throw new CommonException(FollowErrorCode.FOLLOW_NOT_FOUND);
        }
        return follow;
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), size);
    }
}
