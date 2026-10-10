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

    // DB가 아닌 메모리 목록(고정 상품 구성, 계산된 반복 회차 등)을 같은 페이지 형식으로 반환 (요청 page는 1부터)
    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);
        int from = (int) Math.min((long) pageIndex * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return new PageResponse<>(
                all.subList(from, to),
                all.size(),
                (all.size() + pageSize - 1) / pageSize,
                pageSize,
                pageIndex
        );
    }
}
