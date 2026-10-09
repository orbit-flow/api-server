package com.backend.orbitflow.domain.category.dto.response;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.enums.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// ownerUuid : 개인 카테고리면 사용자 uuid, 팀 카테고리면 팀 uuid (내부 id 비노출)
public record CategoryResponse(
        Long id,
        String name,
        String color,
        Visibility visibility,
        boolean teamCategory,
        String ownerUuid,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getColor(),
                category.getVisibility(),
                category.isTeamCategory(),
                category.isTeamCategory() ? category.getTeam().getUuid() : category.getUser().getUuid(),
                category.getCreatedAt()
        );
    }
}
