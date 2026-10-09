package com.backend.orbitflow.domain.block.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record BlockResponse(
        Long id,
        String uuid,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public static BlockResponse of(
            Long id, String uuid, LocalDateTime createdAt
    ) {
        return new BlockResponse(
                id, uuid, createdAt
        );
    }
}
