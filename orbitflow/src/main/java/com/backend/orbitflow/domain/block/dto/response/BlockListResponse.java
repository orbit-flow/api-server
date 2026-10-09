package com.backend.orbitflow.domain.block.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record BlockListResponse(
        Long id,
        String uuid,
        String name,
        String profileImage,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime followAt
) {
}
