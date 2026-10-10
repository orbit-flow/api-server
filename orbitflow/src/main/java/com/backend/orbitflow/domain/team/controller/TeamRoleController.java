package com.backend.orbitflow.domain.team.controller;

import com.backend.orbitflow.domain.team.dto.request.TeamRoleRequest;
import com.backend.orbitflow.domain.team.dto.response.TeamRoleResponse;
import com.backend.orbitflow.domain.team.dto.response.TeamSuccessCode;
import com.backend.orbitflow.domain.team.facade.TeamRoleFacade;
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
@RequestMapping("/api/teams/{teamUuid}/roles")
public class TeamRoleController {

    private final TeamRoleFacade teamRoleFacade;

    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<TeamRoleResponse>>> getRoles(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.GET_ROLE_LIST,
                        teamRoleFacade.getRoles(authUser, teamUuid, page, size)
                ));
    }

    @PostMapping
    public ResponseEntity<CommonResponse<TeamRoleResponse>> createRole(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @Valid @RequestBody TeamRoleRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        TeamSuccessCode.ROLE_CREATE,
                        teamRoleFacade.createRole(authUser, teamUuid, request)
                ));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<CommonResponse<TeamRoleResponse>> updateRole(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @PathVariable Long roleId,
            @Valid @RequestBody TeamRoleRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.ROLE_UPDATE,
                        teamRoleFacade.updateRole(authUser, teamUuid, roleId, request)
                ));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<CommonResponse<Void>> deleteRole(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @PathVariable Long roleId
    ) {
        teamRoleFacade.deleteRole(authUser, teamUuid, roleId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.ROLE_DELETE
                ));
    }

    // 초대 수락 시 자동 부여할 기본 역할 지정
    @PatchMapping("/{roleId}/default")
    public ResponseEntity<CommonResponse<TeamRoleResponse>> updateDefaultRole(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @PathVariable Long roleId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.DEFAULT_ROLE_UPDATE,
                        teamRoleFacade.updateDefaultRole(authUser, teamUuid, roleId)
                ));
    }
}
