package com.backend.orbitflow.domain.team.facade;

import com.backend.orbitflow.domain.team.dto.request.TeamUserRequest;
import com.backend.orbitflow.domain.team.dto.response.TeamInvitationResponse;
import com.backend.orbitflow.domain.team.service.TeamInvitationService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TeamInvitationFacade {

    private final TeamInvitationService teamInvitationService;
    private final TeamService teamService;
    private final UserService userService;

    @Transactional
    public TeamInvitationResponse invite(AuthUser authUser, String teamUuid, TeamUserRequest request) {
        return teamInvitationService.invite(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                userService.getByUuid(request.userUuid())
        );
    }

    @Transactional(readOnly = true)
    public List<TeamInvitationResponse> getTeamInvitations(AuthUser authUser, String teamUuid) {
        return teamInvitationService.getTeamInvitations(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid())
        );
    }

    @Transactional
    public void cancelInvitation(AuthUser authUser, String teamUuid, String invitationUuid) {
        teamInvitationService.cancelInvitation(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                invitationUuid
        );
    }

    @Transactional(readOnly = true)
    public List<TeamInvitationResponse> getMyInvitations(AuthUser authUser) {
        return teamInvitationService.getMyInvitations(userService.getByUuid(authUser.getUuid()));
    }

    @Transactional
    public TeamInvitationResponse acceptInvitation(AuthUser authUser, String invitationUuid) {
        return teamInvitationService.acceptInvitation(userService.getByUuid(authUser.getUuid()), invitationUuid);
    }

    @Transactional
    public TeamInvitationResponse rejectInvitation(AuthUser authUser, String invitationUuid) {
        return teamInvitationService.rejectInvitation(userService.getByUuid(authUser.getUuid()), invitationUuid);
    }
}
