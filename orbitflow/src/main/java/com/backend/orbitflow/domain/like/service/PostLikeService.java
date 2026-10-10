package com.backend.orbitflow.domain.like.service;

import com.backend.orbitflow.domain.like.dto.response.LikeResponse;
import com.backend.orbitflow.domain.like.dto.response.LikerResponse;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

// 게시글 열람 권한은 호출 측(PostLikeFacade)에서 PostService로 확인한 뒤 전달
public interface PostLikeService {

    LikeResponse toggleLike(User actor, Post post);
    Page<LikerResponse> getLikers(User viewer, Post post, int page, int size);

    // 게시글 응답 조립용 (PostService에서 사용)
    long countByPost(Post post, User viewer);
    boolean isLiked(Post post, User viewer);
    Map<Long, Long> countByPostIn(Collection<Post> posts, User viewer);
    Set<Long> findLikedPostIds(Collection<Post> posts, User viewer);
}
