package com.backend.orbitflow.domain.team.facade;

import com.backend.orbitflow.domain.team.dto.request.TeamRequest;
import com.backend.orbitflow.domain.team.dto.response.DeletedTeamResponse;
import com.backend.orbitflow.domain.team.dto.response.TeamResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TeamFacade {

    private final TeamService teamService;
    private final UserService userService;

    public TeamResponse createTeam(AuthUser authUser, TeamRequest request) {
        User user = userService.getByUuid(authUser.getUuid());
        Team team = teamService.createTeam(user, request.name(), request.icon());
        return teamService.getMyTeam(user, team.getUuid());
    }

    public List<TeamResponse> getMyTeams(AuthUser authUser) {
        return teamService.getMyTeams(userService.getByUuid(authUser.getUuid()));
    }

    public TeamResponse getTeam(AuthUser authUser, String uuid) {
        return teamService.getMyTeam(userService.getByUuid(authUser.getUuid()), uuid);
    }

    public TeamResponse updateTeam(AuthUser authUser, String uuid, TeamRequest request) {
        User user = userService.getByUuid(authUser.getUuid());
        teamService.updateTeam(user, uuid, request.name(), request.icon());
        return teamService.getMyTeam(user, uuid);
    }

    public void deleteTeam(AuthUser authUser, String uuid) {
        teamService.deleteTeam(userService.getByUuid(authUser.getUuid()), uuid);
    }

    public List<DeletedTeamResponse> getDeletedTeams(AuthUser authUser) {
        return teamService.getDeletedTeams(userService.getByUuid(authUser.getUuid())).stream()
                .map(team -> DeletedTeamResponse.from(team, TeamService.RETENTION_DAYS))
                .toList();
    }

    public TeamResponse restoreTeam(AuthUser authUser, String uuid) {
        User user = userService.getByUuid(authUser.getUuid());
        teamService.restoreTeam(user, uuid);
        return teamService.getMyTeam(user, uuid);
    }
}
