package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamInvitationResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import org.springframework.data.domain.Page;

public interface TeamInvitationService {

    TeamInvitationResponse invite(Team team, User inviter, User invitee);
    Page<TeamInvitationResponse> getTeamInvitations(Team team, User actor, int page, int size);
    void cancelInvitation(Team team, User actor, String invitationUuid);
    Page<TeamInvitationResponse> getMyInvitations(User me, int page, int size);
    TeamInvitationResponse acceptInvitation(User me, String invitationUuid);
    TeamInvitationResponse rejectInvitation(User me, String invitationUuid);
}
