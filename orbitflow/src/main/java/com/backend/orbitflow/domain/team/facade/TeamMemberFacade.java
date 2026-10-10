package com.backend.orbitflow.domain.team.facade;

import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.team.dto.request.TeamMemberProfileRequest;
import com.backend.orbitflow.domain.team.dto.request.TeamMemberRoleRequest;
import com.backend.orbitflow.domain.team.dto.request.TeamUserRequest;
import com.backend.orbitflow.domain.team.dto.response.TeamMemberResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.team.service.TeamMemberService;
import com.backend.orbitflow.domain.team.service.TeamService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.domain.team.dto.response.TeamRoleResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TeamMemberFacade {

    private final TeamMemberService teamMemberService;
    private final TeamService teamService;
    private final UserService userService;
    private final TeamAuthorityService teamAuthorityService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<TeamMemberResponse> getMembers(AuthUser authUser, String teamUuid, int page, int size) {
        return PageResponse.from(teamMemberService.getMembers(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                page, size
        ));
    }

    @Transactional(readOnly = true)
    public PageResponse<TeamRoleResponse> getMemberRoles(AuthUser authUser, String teamUuid, String userUuid, int page, int size) {
        return PageResponse.from(teamMemberService.getMemberRoles(
                teamService.getActiveTeam(teamUuid),
                userService.getByUuid(authUser.getUuid()),
                userService.getByUuidIncludingBanned(userUuid),
                page, size
        ));
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

    // 탈퇴한 본인과 팀 관리자, 담당 투두를 상속받은 구성원에게 알림
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void leaveTeam(AuthUser authUser, String teamUuid) {
        Team team = teamService.lockActiveTeam(teamService.getActiveTeam(teamUuid));
        User me = userService.getByUuid(authUser.getUuid());
        Map<Long, Integer> inherited = teamMemberService.leaveTeam(team, me);
        notifyInherited(team, me, inherited);
        notifyMemberAndAdmins(team, me, NotificationType.TEAM_LEFT,
                "'" + team.getName() + "' 팀에서 탈퇴했습니다.",
                me.getName() + "님이 '" + team.getName() + "' 팀에서 탈퇴했습니다.");
    }

    // 내보내진 구성원과 팀 관리자, 담당 투두를 상속받은 구성원에게 알림
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void kickMember(AuthUser authUser, String teamUuid, String userUuid) {
        Team team = teamService.lockActiveTeam(teamService.getActiveTeam(teamUuid));
        // 정지된 구성원도 관리할 수 있도록 정지 사용자 포함 조회
        User target = userService.getByUuidIncludingBanned(userUuid);
        Map<Long, Integer> inherited = teamMemberService.kickMember(team, userService.getByUuid(authUser.getUuid()), target);
        notifyInherited(team, target, inherited);
        notifyMemberAndAdmins(team, target, NotificationType.TEAM_LEFT,
                "'" + team.getName() + "' 팀에서 내보내졌습니다.",
                target.getName() + "님이 '" + team.getName() + "' 팀에서 내보내졌습니다.");
    }

    // 역할이 실제로 바뀐 경우에만 변경 대상과 팀 관리자에게 알림
    @Transactional
    public TeamMemberResponse updateMemberRoles(AuthUser authUser, String teamUuid, String userUuid, TeamMemberRoleRequest request) {
        Team team = teamService.getActiveTeam(teamUuid);
        User target = userService.getByUuidIncludingBanned(userUuid);
        Set<Long> before = teamAuthorityService.findRoleIds(team, target);
        TeamMemberResponse response = teamMemberService.updateMemberRoles(
                team, userService.getByUuid(authUser.getUuid()), target, request.roleIds());
        if (!before.equals(new HashSet<>(request.roleIds()))) {
            notifyMemberAndAdmins(team, target, NotificationType.TEAM_ROLE_CHANGED,
                    "'" + team.getName() + "' 팀에서 회원님의 역할이 변경되었습니다.",
                    "'" + team.getName() + "' 팀에서 " + target.getName() + "님의 역할이 변경되었습니다.");
        }
        return response;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void transferOwner(AuthUser authUser, String teamUuid, TeamUserRequest request) {
        teamMemberService.transferOwner(
                teamService.lockActiveTeam(teamService.getActiveTeam(teamUuid)),
                userService.getByUuid(authUser.getUuid()),
                userService.getByUuid(request.userUuid())
        );
    }

    // 변경 대상 본인과 팀 관리자(소유자 또는 MANAGE_TEAM 권한 보유자, 본인 제외)에게
    private void notifyMemberAndAdmins(Team team, User subject, NotificationType type, String subjectContent, String adminContent) {
        eventPublisher.publishEvent(NotificationRequest.to(subject, type, null, null, team.getUuid(), subjectContent));
        List<User> admins = teamAuthorityService.findAdminUsers(team).stream()
                .filter(user -> !user.getId().equals(subject.getId()))
                .toList();
        eventPublisher.publishEvent(NotificationRequest.toAll(admins, type, subject, null, team.getUuid(), adminContent));
    }

    // 팀을 떠난 구성원의 투두 상속 : 상속받은 구성원에게 팀별로 1건 (행위자 없음)
    private void notifyInherited(Team team, User leaver, Map<Long, Integer> inherited) {
        inherited.forEach((heirId, count) -> userService.findById(heirId).ifPresent(heir ->
                eventPublisher.publishEvent(NotificationRequest.to(heir, NotificationType.TODO_ASSIGNED, null, null, team.getUuid(),
                        "'" + team.getName() + "' 팀을 떠난 " + leaver.getName() + "님이 담당하던 투두 " + count + "개가 회원님에게 배정되었습니다."))));
    }
}
