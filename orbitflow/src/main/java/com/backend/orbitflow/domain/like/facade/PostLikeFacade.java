package com.backend.orbitflow.domain.like.facade;

import com.backend.orbitflow.domain.like.dto.response.LikeResponse;
import com.backend.orbitflow.domain.like.dto.response.LikerResponse;
import com.backend.orbitflow.domain.like.service.PostLikeService;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.service.PostService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 게시글 열람 권한(차단·카테고리 공개 범위)을 먼저 확인한 뒤 좋아요 처리
@Component
@RequiredArgsConstructor
public class PostLikeFacade {

    private final PostLikeService postLikeService;
    private final PostService postService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    // 좋아요를 누른 경우에만 게시글 작성자에게 알림 (취소 후 다시 누른 경우는 NotificationService에서 1회만 발송)
    @Transactional
    public LikeResponse toggleLike(AuthUser authUser, Long postId) {
        User me = userService.getByUuid(authUser.getUuid());
        Post post = postService.getViewablePost(me, postId);
        LikeResponse like = postLikeService.toggleLike(me, post);
        if (like.liked()) {
            eventPublisher.publishEvent(NotificationRequest.to(post.getUser(), NotificationType.LIKE, me, post.getId(), null,
                    me.getName() + "님이 게시글을 좋아합니다."));
        }
        return like;
    }

    @Transactional(readOnly = true)
    public PageResponse<LikerResponse> getLikers(AuthUser authUser, Long postId, int page, int size) {
        User me = userService.getByUuid(authUser.getUuid());
        return PageResponse.from(postLikeService.getLikers(me, postService.getViewablePost(me, postId), page, size));
    }
}
