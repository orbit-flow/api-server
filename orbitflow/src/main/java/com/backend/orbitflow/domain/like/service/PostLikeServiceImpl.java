package com.backend.orbitflow.domain.like.service;

import com.backend.orbitflow.domain.like.dto.response.LikeResponse;
import com.backend.orbitflow.domain.like.dto.response.LikerResponse;
import com.backend.orbitflow.domain.like.entity.PostLike;
import com.backend.orbitflow.domain.like.repository.PostLikeRepository;
import com.backend.orbitflow.domain.post.dto.PostCount;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// 게시글을 조회할 수 있는 사용자만 좋아요 가능 (차단 관계가 있으면 게시글 조회 불가, 열람 확인은 PostLikeFacade)
@Service
@RequiredArgsConstructor
@Transactional
public class PostLikeServiceImpl implements PostLikeService {

    private final PostLikeRepository postLikeRepository;

    public LikeResponse toggleLike(User actor, Post post) {
        Optional<PostLike> like = postLikeRepository.findByPostAndUser(post, actor);
        if (like.isPresent()) {
            postLikeRepository.delete(like.get());
        } else {
            postLikeRepository.save(PostLike.of(post, actor));
        }
        return new LikeResponse(post.getId(), like.isEmpty(), postLikeRepository.countByPost(post, actor));
    }

    // 탈퇴한 사용자, 요청자와 차단 관계인 사용자 제외
    @Transactional(readOnly = true)
    public Page<LikerResponse> getLikers(User viewer, Post post, int page, int size) {
        return postLikeRepository.findAllByPost(post, viewer, PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100)))
                .map(LikerResponse::from);
    }

    // 좋아요 수 (목록과 같은 조건 : 탈퇴한 사용자, viewer와 차단 관계인 사용자 제외)
    @Transactional(readOnly = true)
    public long countByPost(Post post, User viewer) {
        return postLikeRepository.countByPost(post, viewer);
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Post post, User viewer) {
        return postLikeRepository.existsByPostAndUser(post, viewer);
    }

    // 게시글별 좋아요 수 (게시글 묶음 단위로 1회 조회, 좋아요가 없는 게시글은 맵에 없음)
    @Transactional(readOnly = true)
    public Map<Long, Long> countByPostIn(Collection<Post> posts, User viewer) {
        return postLikeRepository.countByPostIn(posts, viewer).stream()
                .collect(Collectors.toMap(PostCount::postId, PostCount::count));
    }

    @Transactional(readOnly = true)
    public Set<Long> findLikedPostIds(Collection<Post> posts, User viewer) {
        return postLikeRepository.findLikedPostIds(posts, viewer);
    }
}
