package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamInvitationResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamInvitation;
import com.backend.orbitflow.domain.team.enums.TeamInvitationStatus;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.team.repository.TeamInvitationRepository;
import com.backend.orbitflow.domain.team.repository.TeamMemberRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// 팀 가입은 팀 관리자가 보낸 초대를 사용자가 수락한 시점에만 성립
@Service
@RequiredArgsConstructor
@Transactional
public class TeamInvitationServiceImpl implements TeamInvitationService {

    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamAuthorityService teamAuthorityService;
    private final TeamMemberService teamMemberService;

    public TeamInvitationResponse invite(Team team, User inviter, User invitee) {
        teamAuthorityService.checkPermission(team, inviter, TeamPermission.MANAGE_MEMBERS);
        if (inviter.getId().equals(invitee.getId())) {
            throw new CommonException(TeamErrorCode.SELF_INVITE);
        }
        if (teamMemberRepository.existsByTeamAndUser(team, invitee)) {
            throw new CommonException(TeamErrorCode.ALREADY_TEAM_MEMBER);
        }
        if (teamInvitationRepository.existsByTeamAndInviteeAndStatus(team, invitee, TeamInvitationStatus.PENDING)) {
            throw new CommonException(TeamErrorCode.ALREADY_INVITED);
        }
        TeamInvitation invitation = teamInvitationRepository.save(TeamInvitation.of(
                team,
                inviter,
                invitee,
                UUID.randomUUID().toString().replace("-", ""),
                UUID.randomUUID().toString()
        ));
        return TeamInvitationResponse.from(invitation);
    }

    @Transactional(readOnly = true)
    public List<TeamInvitationResponse> getTeamInvitations(Team team, User actor) {
        teamAuthorityService.checkPermission(team, actor, TeamPermission.MANAGE_MEMBERS);
        return teamInvitationRepository.findPendingByTeam(team).stream()
                .map(TeamInvitationResponse::from)
                .toList();
    }

    // 수락 전 초대는 언제든 즉시 취소 가능, 취소된 초대는 이후 수락 불가
    public void cancelInvitation(Team team, User actor, String invitationUuid) {
        teamAuthorityService.checkPermission(team, actor, TeamPermission.MANAGE_MEMBERS);
        TeamInvitation invitation = getInvitation(invitationUuid);
        if (!invitation.getTeam().getId().equals(team.getId())) {
            throw new CommonException(TeamErrorCode.INVITATION_NOT_FOUND);
        }
        validatePending(invitation);
        invitation.cancel();
    }

    @Transactional(readOnly = true)
    public List<TeamInvitationResponse> getMyInvitations(User me) {
        return teamInvitationRepository.findPendingByInvitee(me).stream()
                .map(TeamInvitationResponse::from)
                .toList();
    }

    public TeamInvitationResponse acceptInvitation(User me, String invitationUuid) {
        TeamInvitation invitation = getMyInvitation(me, invitationUuid);
        validatePending(invitation);
        if (invitation.getTeam().getDeletedAt() != null) {
            throw new CommonException(TeamErrorCode.TEAM_NOT_FOUND);
        }
        teamMemberService.joinTeam(invitation.getTeam(), me);
        invitation.accept();
        return TeamInvitationResponse.from(invitation);
    }

    public TeamInvitationResponse rejectInvitation(User me, String invitationUuid) {
        TeamInvitation invitation = getMyInvitation(me, invitationUuid);
        validatePending(invitation);
        invitation.reject();
        return TeamInvitationResponse.from(invitation);
    }

    private TeamInvitation getInvitation(String invitationUuid) {
        return teamInvitationRepository.findWithAllByUuid(invitationUuid).orElseThrow(
                () -> new CommonException(TeamErrorCode.INVITATION_NOT_FOUND)
        );
    }

    // 다른 사용자의 초대에 접근하면 존재 여부를 노출하지 않도록 NOT_FOUND 처리
    private TeamInvitation getMyInvitation(User me, String invitationUuid) {
        TeamInvitation invitation = getInvitation(invitationUuid);
        if (!invitation.isInvitee(me)) {
            throw new CommonException(TeamErrorCode.INVITATION_NOT_FOUND);
        }
        return invitation;
    }

    private void validatePending(TeamInvitation invitation) {
        if (!invitation.isPending()) {
            throw new CommonException(TeamErrorCode.INVITATION_NOT_PENDING);
        }
    }
}
