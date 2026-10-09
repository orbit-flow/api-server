package com.backend.orbitflow.domain.follow.service;

import com.backend.orbitflow.domain.follow.dto.response.FollowListResponse;
import com.backend.orbitflow.domain.follow.entity.Follow;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;

public interface FollowService {

    FollowState getFollowState(User follower, User followee);
    Page<FollowListResponse> getFollowings(User me, User target, int page, int size, String keyword);
    Page<FollowListResponse> getFollowers(User me, User target, int page, int size, String keyword);
    Page<FollowListResponse> getFollowRequests(User me, int page, int size, String keyword);
    Follow toggleFollow(User follower, User followee);
    Follow acceptFollow(User me, Long followId);
    Follow deniedFollow(User me, Long followId);
    Follow toggleNotification(User follower, User followee);
    void deleteAllBetween(User a, User b);
}
