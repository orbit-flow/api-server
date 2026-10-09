package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;

public interface TeamService {

    int RETENTION_DAYS = 30;

    Team getActiveTeam(String uuid);
    Team createTeam(User owner, String name, String icon);
    List<TeamResponse> getMyTeams(User user);
    TeamResponse getMyTeam(User user, String uuid);
    void updateTeam(User user, String uuid, String name, String icon);
    void deleteTeam(User user, String uuid);
    List<Team> getDeletedTeams(User owner);
    void restoreTeam(User user, String uuid);
}
