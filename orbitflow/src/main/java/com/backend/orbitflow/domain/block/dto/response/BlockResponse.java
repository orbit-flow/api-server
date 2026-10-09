package com.backend.orbitflow.domain.block.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// uuid : 차단 대상 사용자 uuid, 차단 해제된 경우 id·createdAt은 null
public record BlockResponse(
        Long id,
        String uuid,
        boolean blocked,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public static BlockResponse of(
            Long id, String uuid, LocalDateTime createdAt
    ) {
        return new BlockResponse(
                id, uuid, true, createdAt
        );
    }

    public static BlockResponse unblocked(String uuid) {
        return new BlockResponse(
                null, uuid, false, null
        );
    }
}
