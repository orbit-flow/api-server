package com.backend.orbitflow.domain.team.dto.response;

import com.backend.orbitflow.domain.team.entity.TeamInvitation;
import com.backend.orbitflow.domain.team.enums.TeamInvitationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record TeamInvitationResponse(
        String uuid,
        String teamUuid,
        String teamName,
        String teamIcon,
        String inviterName,
        String inviteeUuid,
        String inviteeName,
        TeamInvitationStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public static TeamInvitationResponse from(TeamInvitation invitation) {
        return new TeamInvitationResponse(
                invitation.getUuid(),
                invitation.getTeam().getUuid(),
                invitation.getTeam().getName(),
                invitation.getTeam().getIcon(),
                invitation.getInviter().getName(),
                invitation.getInvitee().getUuid(),
                invitation.getInvitee().getName(),
                invitation.getStatus(),
                invitation.getCreatedAt()
        );
    }
}
