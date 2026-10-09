package com.backend.orbitflow.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// multipart의 "request" 파트 (새 사진은 "images" 파트, 유지할 사진 뒤에 순서대로 추가)
public record PostUpdateRequest(
        @NotBlank(message = "게시글 내용은 비어있을 수 없습니다.")
        @Size(max = 2000, message = "게시글 내용은 2000자 이내여야 합니다.")
        String content,

        // 유지할 기존 사진 URL (표시 순서대로), 빠진 사진은 삭제
        @NotNull(message = "유지할 사진 목록은 비어있을 수 없습니다.")
        List<String> keepImageUrls
) {
}
