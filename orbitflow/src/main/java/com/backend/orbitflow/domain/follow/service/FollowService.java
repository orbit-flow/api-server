package com.backend.orbitflow.domain.follow.service;

import com.backend.orbitflow.domain.follow.dto.response.FollowListResponse;
import com.backend.orbitflow.domain.follow.entity.Follow;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface FollowService {

    FollowState getFollowState(User follower, User followee);
    Set<Long> findFollowerIdsAmong(User user, Collection<Long> userIds);
    Page<FollowListResponse> getFollowings(User me, User target, int page, int size, String keyword);
    Page<FollowListResponse> getFollowers(User me, User target, int page, int size, String keyword);
    Page<FollowListResponse> getFollowRequests(User me, int page, int size, String keyword);
    Follow toggleFollow(User follower, User followee);
    Follow acceptFollow(User me, Long followId);
    Follow deniedFollow(User me, Long followId);
    Follow toggleNotification(User follower, User followee);
    void deleteAllBetween(User a, User b);

    // 알림 발송용 : 알림을 켠 팔로워의 id → uuid (afterId 이후 id 순으로 최대 size명)
    Map<Long, String> findNotifiableFollowers(User followee, long afterId, int size);
}
