package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.Optional;
import java.util.Map;

public interface TeamAuthorityService {

    Optional<TeamMember> findMember(Team team, User user);
    TeamMember getMember(Team team, User user);
    int getPermissionMask(Team team, TeamMember member);
    TeamPermissionSnapshot snapshot(Team team);
    Map<Long, Integer> getPermissionMasksByTeam(User user);
    TeamMember checkPermission(Team team, User user, TeamPermission permission);
    void checkGrantable(Team team, User actor, int mask);
}
