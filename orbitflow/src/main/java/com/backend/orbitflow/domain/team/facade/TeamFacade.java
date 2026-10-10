package com.backend.orbitflow.domain.team.facade;

import com.backend.orbitflow.domain.team.dto.request.TeamRequest;
import com.backend.orbitflow.domain.team.dto.response.DeletedTeamResponse;
import com.backend.orbitflow.domain.team.dto.response.TeamResponse;
import com.backend.orbitflow.domain.category.service.CategoryService;
import com.backend.orbitflow.domain.chat.service.ChatroomService;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.backend.orbitflow.global.common.dto.response.PageResponse;

@Component
@RequiredArgsConstructor
public class TeamFacade {

    private final TeamService teamService;
    private final UserService userService;
    private final TeamAuthorityService teamAuthorityService;
    private final CategoryService categoryService;
    private final ChatroomService chatroomService;

    public TeamResponse createTeam(AuthUser authUser, TeamRequest request) {
        User user = userService.getActiveByUuid(authUser.getUuid());
        Team team = teamService.createTeam(user, request.name(), request.icon());
        return teamService.getMyTeam(user, team.getUuid());
    }

    @Transactional(readOnly = true)
    public PageResponse<TeamResponse> getMyTeams(AuthUser authUser, int page, int size) {
        return PageResponse.from(teamService.getMyTeams(userService.getActiveByUuid(authUser.getUuid()), page, size));
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(AuthUser authUser, String uuid) {
        return teamService.getMyTeam(userService.getActiveByUuid(authUser.getUuid()), uuid);
    }

    // 소유자 또는 MANAGE_TEAM 권한 보유자(팀 관리자)
    @Transactional
    public TeamResponse updateTeam(AuthUser authUser, String uuid, TeamRequest request) {
        User user = userService.getActiveByUuid(authUser.getUuid());
        Team team = teamService.getActiveTeam(uuid);
        teamAuthorityService.checkPermission(team, user, TeamPermission.MANAGE_TEAM);
        teamService.updateTeam(team, request.name(), request.icon());
        return teamService.getMyTeam(user, uuid);
    }

    // 팀 행 잠금 → 구성원 소속 해제를 한 트랜잭션으로 묶음 (팀원 지정 권한은 구성원 소속이 사라지기 전에 삭제)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteTeam(AuthUser authUser, String uuid) {
        Team team = teamService.deleteTeam(userService.getActiveByUuid(authUser.getUuid()), uuid);
        categoryService.deleteAllMemberPermissionsByTeam(team);
        teamService.removeTeamMembers(team);
        chatroomService.removeTeamChatroomMembers(team);
    }

    @Transactional(readOnly = true)
    public PageResponse<DeletedTeamResponse> getDeletedTeams(AuthUser authUser, int page, int size) {
        return PageResponse.from(teamService.getDeletedTeams(userService.getActiveByUuid(authUser.getUuid()), page, size)
                .map(team -> DeletedTeamResponse.from(team, TeamService.RETENTION_DAYS)));
    }

    public TeamResponse restoreTeam(AuthUser authUser, String uuid) {
        User user = userService.getActiveByUuid(authUser.getUuid());
        teamService.restoreTeam(user, uuid);
        return teamService.getMyTeam(user, uuid);
    }
}
