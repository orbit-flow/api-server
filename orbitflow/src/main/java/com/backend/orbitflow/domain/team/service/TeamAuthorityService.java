package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.user.entity.User;

public interface TeamAuthorityService {

    TeamMember getMember(Team team, User user);
    int getPermissionMask(Team team, TeamMember member);
    TeamMember checkPermission(Team team, User user, TeamPermission permission);
    void checkGrantable(Team team, User actor, int mask);
}
