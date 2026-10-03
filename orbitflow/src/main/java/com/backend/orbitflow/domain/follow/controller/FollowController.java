package com.backend.orbitflow.domain.follow.controller;

import com.backend.orbitflow.domain.follow.dto.response.FollowListResponse;
import com.backend.orbitflow.domain.follow.dto.response.FollowResponse;
import com.backend.orbitflow.domain.follow.dto.response.FollowSuccessCode;
import com.backend.orbitflow.domain.follow.facade.FollowFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowFacade followFacade;

    @GetMapping("/{uuid}/followings")
    public ResponseEntity<CommonResponse<PageResponse<FollowListResponse>>> getFollowing(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String uuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        FollowSuccessCode.GET_FOLLOWING_LIST,
                        followFacade.getFollowing(authUser, uuid, page, size, keyword)
                ));
    }

    @GetMapping("/{uuid}/followers")
    public ResponseEntity<CommonResponse<PageResponse<FollowListResponse>>> getFollower(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String uuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        FollowSuccessCode.GET_FOLLOWER_LIST,
                        followFacade.getFollower(authUser, uuid, page, size, keyword)
                ));
    }

    @PostMapping("/{uuid}")
    public ResponseEntity<CommonResponse<FollowResponse>> toggleFollow(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String uuid
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        FollowSuccessCode.FOLLOW_TOGGLE_SUCCESS,
                        followFacade.toggleFollow(authUser, uuid)
                ));
    }

    @PatchMapping("/{followId}")
    public ResponseEntity<CommonResponse<FollowResponse>> acceptFollow(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long followId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        FollowSuccessCode.FOLLOW_ACCEPTED,
                        followFacade.acceptFollow(authUser, followId)
                ));
    }

    @DeleteMapping("/{followId}")
    public ResponseEntity<CommonResponse<FollowResponse>> deleteFollow(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long followId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        FollowSuccessCode.FOLLOW_DENIED,
                        followFacade.deniedFollow(authUser, followId)
                ));
    }
}
