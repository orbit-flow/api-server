package com.backend.orbitflow.domain.team.facade;

import com.backend.orbitflow.domain.team.dto.request.TeamRoleRequest;
import com.backend.orbitflow.domain.team.dto.response.TeamRoleResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.team.service.TeamRoleService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.entity.User;
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
    private final TeamAuthorityService teamAuthorityService;

    @Transactional(readOnly = true)
    public PageResponse<TeamRoleResponse> getRoles(AuthUser authUser, String teamUuid, int page, int size) {
        Team team = teamService.getActiveTeam(teamUuid);
        teamAuthorityService.getMember(team, userService.getActiveByUuid(authUser.getUuid()));
        return PageResponse.from(teamRoleService.getRoles(team, page, size));
    }

    @Transactional
    public TeamRoleResponse createRole(AuthUser authUser, String teamUuid, TeamRoleRequest request) {
        Team team = teamService.getActiveTeam(teamUuid);
        User actor = userService.getActiveByUuid(authUser.getUuid());
        return teamRoleService.createRole(
                team,
                checkManageRoles(team, actor),
                request.name(),
                request.color(),
                request.priority(),
                request.permissions()
        );
    }

    @Transactional
    public TeamRoleResponse updateRole(AuthUser authUser, String teamUuid, Long roleId, TeamRoleRequest request) {
        Team team = teamService.getActiveTeam(teamUuid);
        User actor = userService.getActiveByUuid(authUser.getUuid());
        return teamRoleService.updateRole(
                team,
                checkManageRoles(team, actor),
                roleId,
                request.name(),
                request.color(),
                request.priority(),
                request.permissions()
        );
    }

    @Transactional
    public void deleteRole(AuthUser authUser, String teamUuid, Long roleId) {
        Team team = teamService.getActiveTeam(teamUuid);
        User actor = userService.getActiveByUuid(authUser.getUuid());
        teamRoleService.deleteRole(team, checkManageRoles(team, actor), roleId);
    }

    @Transactional
    public TeamRoleResponse updateDefaultRole(AuthUser authUser, String teamUuid, Long roleId) {
        Team team = teamService.getActiveTeam(teamUuid);
        User actor = userService.getActiveByUuid(authUser.getUuid());
        return teamRoleService.updateDefaultRole(team, checkManageRoles(team, actor), roleId);
    }

    // 역할 관리 권한을 확인하고, 권한 상승 방지에 쓸 요청자의 권한 mask를 반환
    private int checkManageRoles(Team team, User actor) {
        TeamMember member = teamAuthorityService.checkPermission(team, actor, TeamPermission.MANAGE_ROLES);
        return teamAuthorityService.getPermissionMask(team, member);
    }
}
