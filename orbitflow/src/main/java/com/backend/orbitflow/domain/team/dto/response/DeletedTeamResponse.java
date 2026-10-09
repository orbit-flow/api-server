package com.backend.orbitflow.domain.team.dto.response;

import com.backend.orbitflow.domain.team.entity.Team;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// 복구 가능한(삭제 후 30일 이내) 팀
public record DeletedTeamResponse(
        String uuid,
        String name,
        String icon,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime deletedAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime restorableUntil
) {

    public static DeletedTeamResponse from(Team team, int retentionDays) {
        return new DeletedTeamResponse(
                team.getUuid(),
                team.getName(),
                team.getIcon(),
                team.getDeletedAt(),
                team.getDeletedAt().plusDays(retentionDays)
        );
    }
}
