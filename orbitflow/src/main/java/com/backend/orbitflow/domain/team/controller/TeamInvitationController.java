package com.backend.orbitflow.domain.team.controller;

import com.backend.orbitflow.domain.team.dto.request.TeamUserRequest;
import com.backend.orbitflow.domain.team.dto.response.TeamInvitationResponse;
import com.backend.orbitflow.domain.team.dto.response.TeamSuccessCode;
import com.backend.orbitflow.domain.team.facade.TeamInvitationFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.backend.orbitflow.global.common.dto.response.PageResponse;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TeamInvitationController {

    private final TeamInvitationFacade teamInvitationFacade;

    // ---------- 팀 관리자 ----------

    @PostMapping("/teams/{teamUuid}/invitations")
    public ResponseEntity<CommonResponse<TeamInvitationResponse>> invite(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @Valid @RequestBody TeamUserRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        TeamSuccessCode.INVITATION_CREATE,
                        teamInvitationFacade.invite(authUser, teamUuid, request)
                ));
    }

    // 팀이 보낸 대기 중인 초대 목록
    @GetMapping("/teams/{teamUuid}/invitations")
    public ResponseEntity<CommonResponse<PageResponse<TeamInvitationResponse>>> getTeamInvitations(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.GET_INVITATION_LIST,
                        teamInvitationFacade.getTeamInvitations(authUser, teamUuid, page, size)
                ));
    }

    @DeleteMapping("/teams/{teamUuid}/invitations/{invitationUuid}")
    public ResponseEntity<CommonResponse<Void>> cancelInvitation(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @PathVariable String invitationUuid
    ) {
        teamInvitationFacade.cancelInvitation(authUser, teamUuid, invitationUuid);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.INVITATION_CANCEL
                ));
    }

    // ---------- 초대받은 사용자 ----------

    // 내가 받은 대기 중인 초대 목록
    @GetMapping("/team-invitations")
    public ResponseEntity<CommonResponse<PageResponse<TeamInvitationResponse>>> getMyInvitations(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.GET_INVITATION_LIST,
                        teamInvitationFacade.getMyInvitations(authUser, page, size)
                ));
    }

    @PostMapping("/team-invitations/{invitationUuid}/accept")
    public ResponseEntity<CommonResponse<TeamInvitationResponse>> acceptInvitation(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String invitationUuid
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.INVITATION_ACCEPT,
                        teamInvitationFacade.acceptInvitation(authUser, invitationUuid)
                ));
    }

    @PostMapping("/team-invitations/{invitationUuid}/reject")
    public ResponseEntity<CommonResponse<TeamInvitationResponse>> rejectInvitation(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String invitationUuid
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.INVITATION_REJECT,
                        teamInvitationFacade.rejectInvitation(authUser, invitationUuid)
                ));
    }
}
