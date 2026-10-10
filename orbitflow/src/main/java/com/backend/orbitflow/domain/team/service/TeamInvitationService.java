package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamInvitationResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamInvitation;
import com.backend.orbitflow.domain.user.entity.User;

import org.springframework.data.domain.Page;

public interface TeamInvitationService {

    TeamInvitationResponse invite(Team team, User inviter, User invitee);
    Page<TeamInvitationResponse> getTeamInvitations(Team team, int page, int size);
    void cancelInvitation(Team team, String invitationUuid);
    Page<TeamInvitationResponse> getMyInvitations(User me, int page, int size);
    // 수락은 호출 측이 getAcceptableInvitation → (팀 행 잠금·구성원 가입) → acceptInvitation 순서로 진행
    TeamInvitation getAcceptableInvitation(User me, String invitationUuid);
    TeamInvitationResponse acceptInvitation(TeamInvitation invitation);
    TeamInvitationResponse rejectInvitation(User me, String invitationUuid);
}
