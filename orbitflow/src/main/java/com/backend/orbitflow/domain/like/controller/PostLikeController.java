package com.backend.orbitflow.domain.like.controller;

import com.backend.orbitflow.domain.like.dto.response.LikeResponse;
import com.backend.orbitflow.domain.like.dto.response.LikeSuccessCode;
import com.backend.orbitflow.domain.like.dto.response.LikerResponse;
import com.backend.orbitflow.domain.like.facade.PostLikeFacade;
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
@RequestMapping("/api/posts/{postId}/likes")
public class PostLikeController {

    private final PostLikeFacade postLikeFacade;

    // 좋아요 / 좋아요 취소 토글
    @PostMapping
    public ResponseEntity<CommonResponse<LikeResponse>> toggleLike(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        LikeSuccessCode.LIKE_TOGGLE,
                        postLikeFacade.toggleLike(authUser, postId)
                ));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<LikerResponse>>> getLikers(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        LikeSuccessCode.GET_LIKER_LIST,
                        postLikeFacade.getLikers(authUser, postId, page, size)
                ));
    }
}
