package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamRoleResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;

public interface TeamRoleService {

    Page<TeamRoleResponse> getRoles(Team team, User me, int page, int size);
    TeamRoleResponse createRole(Team team, User actor, String name, String color, int priority, List<TeamPermission> permissions);
    TeamRoleResponse updateRole(Team team, User actor, Long roleId, String name, String color, int priority, List<TeamPermission> permissions);
    void deleteRole(Team team, User actor, Long roleId);
    TeamRoleResponse updateDefaultRole(Team team, User actor, Long roleId);
    List<TeamRole> findRoles(Team team, Collection<Long> roleIds);
}
