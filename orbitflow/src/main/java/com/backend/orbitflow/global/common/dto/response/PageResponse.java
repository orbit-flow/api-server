package com.backend.orbitflow.global.common.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int size,
        int number
) {
    // Page -> PageResponse 변환 메서드
    public static <T> PageResponse<T> from(Page<T> pagedData) {
        return new PageResponse<>(
                pagedData.getContent(),
                pagedData.getTotalElements(),
                pagedData.getTotalPages(),
                pagedData.getPageable().getPageSize(),
                pagedData.getPageable().getPageNumber()
        );
    }
}
