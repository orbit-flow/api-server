package com.backend.orbitflow.domain.team.facade;

import com.backend.orbitflow.domain.team.dto.request.TeamRoleRequest;
import com.backend.orbitflow.domain.team.dto.response.TeamRoleResponse;
import com.backend.orbitflow.domain.team.service.TeamRoleService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.backend.orbitflow.global.common.dto.response.PageResponse;

@Component
@RequiredArgsConstructor
public class TeamRoleFacade {

    private final TeamRoleService teamRoleService;
    private final TeamService teamService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public PageResponse<TeamRoleResponse> getRoles(AuthUser authUser, String teamUuid, int page, int size) {
        return PageResponse.from(teamRoleService.getRoles(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                page, size
        ));
    }

    @Transactional
    public TeamRoleResponse createRole(AuthUser authUser, String teamUuid, TeamRoleRequest request) {
        return teamRoleService.createRole(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                request.name(),
                request.color(),
                request.priority(),
                request.permissions()
        );
    }

    @Transactional
    public TeamRoleResponse updateRole(AuthUser authUser, String teamUuid, Long roleId, TeamRoleRequest request) {
        return teamRoleService.updateRole(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                roleId,
                request.name(),
                request.color(),
                request.priority(),
                request.permissions()
        );
    }

    @Transactional
    public void deleteRole(AuthUser authUser, String teamUuid, Long roleId) {
        teamRoleService.deleteRole(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                roleId
        );
    }

    @Transactional
    public TeamRoleResponse updateDefaultRole(AuthUser authUser, String teamUuid, Long roleId) {
        return teamRoleService.updateDefaultRole(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                roleId
        );
    }
}
