package com.backend.orbitflow.domain.category.dto;

// 카테고리 열람 허용 한 줄 : roleId와 memberId 중 하나만 값이 있음
public record CategoryGrant(Long categoryId, Long roleId, Long memberId) {
}
