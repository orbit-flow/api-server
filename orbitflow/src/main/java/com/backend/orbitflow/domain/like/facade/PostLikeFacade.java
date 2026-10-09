package com.backend.orbitflow.domain.like.facade;

import com.backend.orbitflow.domain.like.dto.response.LikeResponse;
import com.backend.orbitflow.domain.like.dto.response.LikerResponse;
import com.backend.orbitflow.domain.like.service.PostLikeService;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PostLikeFacade {

    private final PostLikeService postLikeService;
    private final UserService userService;

    @Transactional
    public LikeResponse toggleLike(AuthUser authUser, Long postId) {
        return postLikeService.toggleLike(userService.getByUuid(authUser.getUuid()), postId);
    }

    @Transactional(readOnly = true)
    public PageResponse<LikerResponse> getLikers(AuthUser authUser, Long postId, int page, int size) {
        return PageResponse.from(postLikeService.getLikers(userService.getByUuid(authUser.getUuid()), postId, page, size));
    }
}
