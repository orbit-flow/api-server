package com.backend.orbitflow.domain.team.controller;

import com.backend.orbitflow.domain.team.dto.request.TeamRequest;
import com.backend.orbitflow.domain.team.dto.response.DeletedTeamResponse;
import com.backend.orbitflow.domain.team.dto.response.TeamResponse;
import com.backend.orbitflow.domain.team.dto.response.TeamSuccessCode;
import com.backend.orbitflow.domain.team.facade.TeamFacade;
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
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamFacade teamFacade;

    @PostMapping
    public ResponseEntity<CommonResponse<TeamResponse>> createTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody TeamRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        TeamSuccessCode.TEAM_CREATE,
                        teamFacade.createTeam(authUser, request)
                ));
    }

    // 내가 소속된 팀 목록
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<TeamResponse>>> getMyTeams(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.GET_TEAM_LIST,
                        teamFacade.getMyTeams(authUser, page, size)
                ));
    }

    // 내가 소유한 팀 중 복구 가능한(삭제 후 30일 이내) 팀 목록
    @GetMapping("/deleted")
    public ResponseEntity<CommonResponse<PageResponse<DeletedTeamResponse>>> getDeletedTeams(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.GET_DELETED_TEAM_LIST,
                        teamFacade.getDeletedTeams(authUser, page, size)
                ));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<CommonResponse<TeamResponse>> getTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String uuid
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.GET_TEAM_INFO,
                        teamFacade.getTeam(authUser, uuid)
                ));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<CommonResponse<TeamResponse>> updateTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String uuid,
            @Valid @RequestBody TeamRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.TEAM_UPDATE,
                        teamFacade.updateTeam(authUser, uuid, request)
                ));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<CommonResponse<Void>> deleteTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String uuid
    ) {
        teamFacade.deleteTeam(authUser, uuid);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.TEAM_DELETE
                ));
    }

    @PostMapping("/{uuid}/restore")
    public ResponseEntity<CommonResponse<TeamResponse>> restoreTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String uuid
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.TEAM_RESTORE,
                        teamFacade.restoreTeam(authUser, uuid)
                ));
    }
}
