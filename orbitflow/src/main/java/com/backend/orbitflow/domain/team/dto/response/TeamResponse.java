package com.backend.orbitflow.domain.team.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record TeamResponse(
        String uuid,
        String name,
        String icon,
        String ownerName,
        long memberCount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
}
