package com.backend.orbitflow.domain.post.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {

    POST_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 게시글입니다.",
            "https://orbitflow.com/errors/post-not-found",
            "Post Not Found"),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN,
            "게시글을 조회할 권한이 없습니다.",
            "https://orbitflow.com/errors/post-access-denied",
            "Post Access Denied"),
    POST_WRITE_DENIED(HttpStatus.FORBIDDEN,
            "본인의 개인 투두 또는 담당 중인 팀 투두에만 게시글을 작성할 수 있습니다.",
            "https://orbitflow.com/errors/post-write-denied",
            "Post Write Denied"),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN,
            "게시글 작성자만 수행할 수 있습니다.",
            "https://orbitflow.com/errors/not-post-author",
            "Not Post Author"),
    IMAGE_COUNT_EXCEED(HttpStatus.BAD_REQUEST,
            "게시글 사진은 최대 3장까지 첨부할 수 있습니다.",
            "https://orbitflow.com/errors/image-count-exceed",
            "Image Count Exceed"),
    INVALID_KEEP_IMAGE(HttpStatus.BAD_REQUEST,
            "유지할 사진은 이 게시글에 첨부된 사진이어야 합니다.",
            "https://orbitflow.com/errors/invalid-keep-image",
            "Invalid Keep Image");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
