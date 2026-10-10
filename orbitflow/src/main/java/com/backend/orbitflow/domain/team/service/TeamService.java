package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import org.springframework.data.domain.Page;

public interface TeamService {

    int RETENTION_DAYS = 30;

    Team getActiveTeam(String uuid);
    Team lockActiveTeam(Team team);
    Team createTeam(User owner, String name, String icon);
    Page<TeamResponse> getMyTeams(User user, int page, int size);
    TeamResponse getMyTeam(User user, String uuid);
    void updateTeam(User user, String uuid, String name, String icon);
    void deleteTeam(User user, String uuid);
    Page<Team> getDeletedTeams(User owner, int page, int size);
    void restoreTeam(User user, String uuid);
    boolean ownsActiveTeam(User user);
}
