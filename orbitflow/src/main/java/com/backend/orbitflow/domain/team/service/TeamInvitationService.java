package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamInvitationResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;

public interface TeamInvitationService {

    TeamInvitationResponse invite(Team team, User inviter, User invitee);
    List<TeamInvitationResponse> getTeamInvitations(Team team, User actor);
    void cancelInvitation(Team team, User actor, String invitationUuid);
    List<TeamInvitationResponse> getMyInvitations(User me);
    TeamInvitationResponse acceptInvitation(User me, String invitationUuid);
    TeamInvitationResponse rejectInvitation(User me, String invitationUuid);
}
