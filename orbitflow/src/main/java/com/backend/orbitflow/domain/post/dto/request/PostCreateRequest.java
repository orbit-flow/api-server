package com.backend.orbitflow.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// multipart의 "request" 파트 (사진은 "images" 파트)
public record PostCreateRequest(
        @NotBlank(message = "게시글 내용은 비어있을 수 없습니다.")
        @Size(max = 2000, message = "게시글 내용은 2000자 이내여야 합니다.")
        String content
) {
}
