package com.backend.orbitflow.domain.team.facade;

import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamInvitation;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.team.service.TeamMemberService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.team.dto.request.TeamUserRequest;
import com.backend.orbitflow.domain.team.dto.response.TeamInvitationResponse;
import com.backend.orbitflow.domain.team.service.TeamInvitationService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.backend.orbitflow.global.common.dto.response.PageResponse;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TeamInvitationFacade {

    private final TeamInvitationService teamInvitationService;
    private final TeamService teamService;
    private final TeamMemberService teamMemberService;
    private final UserService userService;
    private final TeamAuthorityService teamAuthorityService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TeamInvitationResponse invite(AuthUser authUser, String teamUuid, TeamUserRequest request) {
        Team team = teamService.getActiveTeam(teamUuid);
        User inviter = userService.getActiveByUuid(authUser.getUuid());
        User invitee = userService.getByUuid(request.userUuid());
        teamAuthorityService.checkPermission(team, inviter, TeamPermission.MANAGE_MEMBERS);
        return teamInvitationService.invite(team, inviter, invitee);
    }

    @Transactional(readOnly = true)
    public PageResponse<TeamInvitationResponse> getTeamInvitations(AuthUser authUser, String teamUuid, int page, int size) {
        Team team = teamService.getActiveTeam(teamUuid);
        teamAuthorityService.checkPermission(team, userService.getActiveByUuid(authUser.getUuid()), TeamPermission.MANAGE_MEMBERS);
        return PageResponse.from(teamInvitationService.getTeamInvitations(team, page, size));
    }

    @Transactional
    public void cancelInvitation(AuthUser authUser, String teamUuid, String invitationUuid) {
        Team team = teamService.getActiveTeam(teamUuid);
        teamAuthorityService.checkPermission(team, userService.getActiveByUuid(authUser.getUuid()), TeamPermission.MANAGE_MEMBERS);
        teamInvitationService.cancelInvitation(team, invitationUuid);
    }

    @Transactional(readOnly = true)
    public PageResponse<TeamInvitationResponse> getMyInvitations(AuthUser authUser, int page, int size) {
        return PageResponse.from(teamInvitationService.getMyInvitations(userService.getActiveByUuid(authUser.getUuid()), page, size));
    }

    // 가입한 본인과 팀 관리자(본인 제외)에게 알림
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TeamInvitationResponse acceptInvitation(AuthUser authUser, String invitationUuid) {
        User me = userService.getActiveByUuid(authUser.getUuid());
        TeamInvitation invitation = teamInvitationService.getAcceptableInvitation(me, invitationUuid);
        // 팀 행을 잠가 동시에 진행되는 팀 삭제와 직렬화 (삭제된 팀에 구성원이 남지 않도록)
        teamMemberService.joinTeam(teamService.lockActiveTeam(invitation.getTeam()), me);
        TeamInvitationResponse response = teamInvitationService.acceptInvitation(invitation);
        Team team = teamService.getActiveTeam(response.teamUuid());
        eventPublisher.publishEvent(NotificationRequest.to(me, NotificationType.TEAM_JOINED, null, null, team.getUuid(),
                "'" + team.getName() + "' 팀에 가입했습니다."));
        List<User> admins = teamAuthorityService.findAdminUsers(team).stream()
                .filter(user -> !user.getId().equals(me.getId()))
                .toList();
        eventPublisher.publishEvent(NotificationRequest.toAll(admins, NotificationType.TEAM_JOINED, me, null, team.getUuid(),
                me.getName() + "님이 '" + team.getName() + "' 팀에 가입했습니다."));
        return response;
    }

    @Transactional
    public TeamInvitationResponse rejectInvitation(AuthUser authUser, String invitationUuid) {
        return teamInvitationService.rejectInvitation(userService.getActiveByUuid(authUser.getUuid()), invitationUuid);
    }
}
