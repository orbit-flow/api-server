package com.backend.orbitflow.domain.follow.service;

import com.backend.orbitflow.domain.block.service.BlockService;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Collection;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final BlockService blockService;

    // follows row가 없으면 NOT_FOLLOW
    // userIds 중 user를 팔로우 중인 사용자 id (한 번에 조회)
    @Transactional(readOnly = true)
    public Set<Long> findFollowerIdsAmong(User user, Collection<Long> userIds) {
        return userIds.isEmpty() ? Set.of() : followRepository.findFollowerIdsAmong(user, userIds);
    }

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
    // 파사드의 차단 확인과 저장 사이에 차단이 생길 수 있으므로 저장 후 같은 트랜잭션에서 다시 확인 (차단이면 롤백)
    // 다른 트랜잭션이 커밋한 차단이 보이도록 READ_COMMITTED 필수
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Follow toggleFollow(User follower, User followee) {
        Optional<Follow> follow = followRepository.findByFollowerAndFollowee(follower, followee);
        if (follow.isPresent()) {
            followRepository.delete(follow.get());
            return null;
        }
        Follow saved = followRepository.save(Follow.of(followee, follower));
        if (blockService.isBlocked(follower, followee)) {
            throw new CommonException(FollowErrorCode.BLOCKED_USER);
        }
        return saved;
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

    // 팔로워 수와 무관하게 메모리를 쓰지 않도록 발송에 필요한 id·uuid만 조회 (탈퇴·정지 사용자 제외)
    @Transactional(readOnly = true)
    public Map<Long, String> findNotifiableFollowers(User followee, long afterId, int size) {
        Map<Long, String> followers = new LinkedHashMap<>();
        followRepository.findNotifiableFollowers(followee, afterId, PageRequest.of(0, size))
                .forEach(follower -> followers.put(follower.getId(), follower.getUuid()));
        return followers;
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
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
