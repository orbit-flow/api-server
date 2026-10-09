package com.backend.orbitflow.domain.post.controller;

import com.backend.orbitflow.domain.post.dto.request.PostCreateRequest;
import com.backend.orbitflow.domain.post.dto.request.PostUpdateRequest;
import com.backend.orbitflow.domain.post.dto.response.PostResponse;
import com.backend.orbitflow.domain.post.dto.response.PostSuccessCode;
import com.backend.orbitflow.domain.post.facade.PostFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PostController {

    private final PostFacade postFacade;

    // multipart : request(JSON) + images(최대 3장, 선택)
    @PostMapping(value = "/todos/{todoId}/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<PostResponse>> createPost(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId,
            @Valid @RequestPart("request") PostCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        PostSuccessCode.POST_CREATE,
                        postFacade.createPost(authUser, todoId, request, images)
                ));
    }

    @GetMapping("/todos/{todoId}/posts")
    public ResponseEntity<CommonResponse<PageResponse<PostResponse>>> getTodoPosts(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PostSuccessCode.GET_POST_LIST,
                        postFacade.getTodoPosts(authUser, todoId, page, size)
                ));
    }

    // 사용자의 게시글 중 내가 열람 가능한 것만
    @GetMapping("/users/{userUuid}/posts")
    public ResponseEntity<CommonResponse<PageResponse<PostResponse>>> getUserPosts(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String userUuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PostSuccessCode.GET_POST_LIST,
                        postFacade.getUserPosts(authUser, userUuid, page, size)
                ));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<CommonResponse<PostResponse>> getPost(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PostSuccessCode.GET_POST_INFO,
                        postFacade.getPost(authUser, postId)
                ));
    }

    // multipart : request(JSON, 유지할 사진 URL 포함) + images(새 사진, 선택)
    @PutMapping(value = "/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<PostResponse>> updatePost(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId,
            @Valid @RequestPart("request") PostUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PostSuccessCode.POST_UPDATE,
                        postFacade.updatePost(authUser, postId, request, images)
                ));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<CommonResponse<Void>> deletePost(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId
    ) {
        postFacade.deletePost(authUser, postId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        PostSuccessCode.POST_DELETE
                ));
    }
}
