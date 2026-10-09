package com.backend.orbitflow.domain.like.service;

import com.backend.orbitflow.domain.like.dto.response.LikeResponse;
import com.backend.orbitflow.domain.like.dto.response.LikerResponse;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;

public interface PostLikeService {

    LikeResponse toggleLike(User actor, Long postId);
    Page<LikerResponse> getLikers(User viewer, Long postId, int page, int size);
}
