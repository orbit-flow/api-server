package com.backend.orbitflow.domain.post.facade;

import com.backend.orbitflow.domain.post.dto.request.PostCreateRequest;
import com.backend.orbitflow.domain.post.dto.request.PostUpdateRequest;
import com.backend.orbitflow.domain.post.dto.response.PostResponse;
import com.backend.orbitflow.domain.post.service.PostService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostFacade {

    private final PostService postService;
    private final UserService userService;

    @Transactional
    public PostResponse createPost(AuthUser authUser, Long todoId, PostCreateRequest request, List<MultipartFile> images) {
        return postService.createPost(me(authUser), todoId, request.content(), images);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(AuthUser authUser, Long postId) {
        return postService.getPost(me(authUser), postId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getTodoPosts(AuthUser authUser, Long todoId, int page, int size) {
        return PageResponse.from(postService.getTodoPosts(me(authUser), todoId, page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getUserPosts(AuthUser authUser, String userUuid, int page, int size) {
        return PageResponse.from(postService.getUserPosts(me(authUser), userService.getByUuid(userUuid), page, size));
    }

    @Transactional
    public PostResponse updatePost(AuthUser authUser, Long postId, PostUpdateRequest request, List<MultipartFile> images) {
        return postService.updatePost(me(authUser), postId, request.content(), request.keepImageUrls(), images);
    }

    @Transactional
    public void deletePost(AuthUser authUser, Long postId) {
        postService.deletePost(me(authUser), postId);
    }

    private User me(AuthUser authUser) {
        return userService.getByUuid(authUser.getUuid());
    }
}
