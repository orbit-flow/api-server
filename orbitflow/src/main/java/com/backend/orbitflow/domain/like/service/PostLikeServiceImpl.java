package com.backend.orbitflow.domain.like.service;

import com.backend.orbitflow.domain.like.dto.response.LikeResponse;
import com.backend.orbitflow.domain.like.dto.response.LikerResponse;
import com.backend.orbitflow.domain.like.entity.PostLike;
import com.backend.orbitflow.domain.like.repository.PostLikeRepository;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.service.PostService;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// 게시글을 조회할 수 있는 사용자만 좋아요 가능 (차단 관계가 있으면 게시글 조회 불가)
@Service
@RequiredArgsConstructor
@Transactional
public class PostLikeServiceImpl implements PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostService postService;

    public LikeResponse toggleLike(User actor, Long postId) {
        Post post = postService.getViewablePost(actor, postId);
        Optional<PostLike> like = postLikeRepository.findByPostAndUser(post, actor);
        if (like.isPresent()) {
            postLikeRepository.delete(like.get());
        } else {
            postLikeRepository.save(PostLike.of(post, actor));
            // TODO: 알림 도메인 구현 후 게시글 작성자에게 LIKE 알림 발송 (본인 게시글 제외)
        }
        return new LikeResponse(post.getId(), like.isEmpty(), postLikeRepository.countByPost(post));
    }

    // 탈퇴한 사용자, 요청자와 차단 관계인 사용자 제외
    @Transactional(readOnly = true)
    public Page<LikerResponse> getLikers(User viewer, Long postId, int page, int size) {
        Post post = postService.getViewablePost(viewer, postId);
        return postLikeRepository.findAllByPost(post, viewer, PageRequest.of(Math.max(page - 1, 0), size))
                .map(LikerResponse::from);
    }
}
