package com.backend.orbitflow.domain.comment.controller;

import com.backend.orbitflow.domain.comment.dto.request.CommentCreateRequest;
import com.backend.orbitflow.domain.comment.dto.request.CommentUpdateRequest;
import com.backend.orbitflow.domain.comment.dto.response.CommentResponse;
import com.backend.orbitflow.domain.comment.dto.response.CommentSuccessCode;
import com.backend.orbitflow.domain.comment.facade.CommentFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentFacade commentFacade;

    // parentCommentId가 있으면 대댓글
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommonResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        CommentSuccessCode.COMMENT_CREATE,
                        commentFacade.createComment(authUser, postId, request)
                ));
    }

    // 최상위 댓글 페이지 (대댓글은 개수만, 본문은 /comments/{commentId}/replies)
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<CommonResponse<PageResponse<CommentResponse>>> getComments(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        CommentSuccessCode.GET_COMMENT_LIST,
                        commentFacade.getComments(authUser, postId, page, size)
                ));
    }

    // 대댓글 페이지 (작성 순)
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<CommonResponse<PageResponse<CommentResponse>>> getReplies(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        CommentSuccessCode.GET_COMMENT_LIST,
                        commentFacade.getReplies(authUser, commentId, page, size)
                ));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommonResponse<CommentResponse>> updateComment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        CommentSuccessCode.COMMENT_UPDATE,
                        commentFacade.updateComment(authUser, commentId, request)
                ));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<CommonResponse<Void>> deleteComment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long commentId
    ) {
        commentFacade.deleteComment(authUser, commentId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        CommentSuccessCode.COMMENT_DELETE
                ));
    }
}
