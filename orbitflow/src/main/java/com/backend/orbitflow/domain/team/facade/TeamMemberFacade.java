package com.backend.orbitflow.domain.team.facade;

import com.backend.orbitflow.domain.team.dto.request.TeamMemberProfileRequest;
import com.backend.orbitflow.domain.team.dto.request.TeamMemberRoleRequest;
import com.backend.orbitflow.domain.team.dto.request.TeamUserRequest;
import com.backend.orbitflow.domain.team.dto.response.TeamMemberResponse;
import com.backend.orbitflow.domain.team.service.TeamMemberService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TeamMemberFacade {

    private final TeamMemberService teamMemberService;
    private final TeamService teamService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getMembers(AuthUser authUser, String teamUuid) {
        return teamMemberService.getMembers(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid())
        );
    }

    @Transactional(readOnly = true)
    public TeamMemberResponse getMyMemberInfo(AuthUser authUser, String teamUuid) {
        return teamMemberService.getMyMemberInfo(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid())
        );
    }

    @Transactional
    public TeamMemberResponse updateMyProfile(AuthUser authUser, String teamUuid, TeamMemberProfileRequest request) {
        return teamMemberService.updateMyProfile(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                request.nickname(),
                request.bio()
        );
    }

    @Transactional
    public void leaveTeam(AuthUser authUser, String teamUuid) {
        teamMemberService.leaveTeam(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid())
        );
    }

    @Transactional
    public void kickMember(AuthUser authUser, String teamUuid, String userUuid) {
        teamMemberService.kickMember(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                userService.getByUuid(userUuid)
        );
    }

    @Transactional
    public TeamMemberResponse updateMemberRoles(AuthUser authUser, String teamUuid, String userUuid, TeamMemberRoleRequest request) {
        return teamMemberService.updateMemberRoles(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                userService.getByUuid(userUuid),
                request.roleIds()
        );
    }

    @Transactional
    public void transferOwner(AuthUser authUser, String teamUuid, TeamUserRequest request) {
        teamMemberService.transferOwner(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                userService.getByUuid(request.userUuid())
        );
    }
}
