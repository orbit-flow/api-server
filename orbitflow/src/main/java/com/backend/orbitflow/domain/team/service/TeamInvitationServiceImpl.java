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

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

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
        // 팀 행을 잠가 같은 팀의 동시 초대가 중복 확인을 함께 통과하지 못하게 함
        teamInvitationRepository.findTeamForUpdate(team.getId());
        if (teamMemberRepository.existsByTeamAndUser(team, invitee)) {
            throw new CommonException(TeamErrorCode.ALREADY_TEAM_MEMBER);
        }
        // 만료된 대기 초대는 중복으로 보지 않아 다시 초대 가능
        if (teamInvitationRepository.existsByTeamAndInviteeAndStatusAndCreatedAtAfter(team, invitee, TeamInvitationStatus.PENDING, expirationCutoff())) {
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
    public Page<TeamInvitationResponse> getTeamInvitations(Team team, User actor, int page, int size) {
        Pageable pageable = toPageable(page, size);
        teamAuthorityService.checkPermission(team, actor, TeamPermission.MANAGE_MEMBERS);
        return teamInvitationRepository.findPendingByTeam(team, expirationCutoff(), pageable).map(TeamInvitationResponse::from);
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
    public Page<TeamInvitationResponse> getMyInvitations(User me, int page, int size) {
        Pageable pageable = toPageable(page, size);
        return teamInvitationRepository.findPendingByInvitee(me, expirationCutoff(), pageable).map(TeamInvitationResponse::from);
    }

    public TeamInvitationResponse acceptInvitation(User me, String invitationUuid) {
        TeamInvitation invitation = getMyInvitation(me, invitationUuid);
        validatePending(invitation);
        validateNotExpired(invitation);
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
        validateNotExpired(invitation);
        invitation.reject();
        return TeamInvitationResponse.from(invitation);
    }

    // 수락·거절·취소는 초대 행을 잠근 뒤 상태를 검증하므로 같은 초대가 동시에 두 번 처리되지 않음
    private TeamInvitation getInvitation(String invitationUuid) {
        return teamInvitationRepository.findByUuidForUpdate(invitationUuid).orElseThrow(
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

    // 생성 후 7일이 지난 대기 초대는 수락·거절 불가
    private void validateNotExpired(TeamInvitation invitation) {
        if (invitation.isExpired()) {
            throw new CommonException(TeamErrorCode.INVITATION_EXPIRED);
        }
    }

    // 이 시각 이후에 생성된 대기 초대만 유효
    private LocalDateTime expirationCutoff() {
        return LocalDateTime.now().minusDays(TeamInvitation.EXPIRATION_DAYS);
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
