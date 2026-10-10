package com.backend.orbitflow.domain.team.controller;

import com.backend.orbitflow.domain.team.dto.request.TeamMemberProfileRequest;
import com.backend.orbitflow.domain.team.dto.request.TeamMemberRoleRequest;
import com.backend.orbitflow.domain.team.dto.request.TeamUserRequest;
import com.backend.orbitflow.domain.team.dto.response.TeamMemberResponse;
import com.backend.orbitflow.domain.team.dto.response.TeamSuccessCode;
import com.backend.orbitflow.domain.team.facade.TeamMemberFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.backend.orbitflow.domain.team.dto.response.TeamRoleResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams/{teamUuid}")
public class TeamMemberController {

    private final TeamMemberFacade teamMemberFacade;

    @GetMapping("/members")
    public ResponseEntity<CommonResponse<PageResponse<TeamMemberResponse>>> getMembers(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.GET_MEMBER_LIST,
                        teamMemberFacade.getMembers(authUser, teamUuid, page, size)
                ));
    }

    // 구성원 한 명의 전체 역할 (우선순위 높은 순), 구성원 목록에는 대표 역할과 개수만 포함
    @GetMapping("/members/{userUuid}/roles")
    public ResponseEntity<CommonResponse<PageResponse<TeamRoleResponse>>> getMemberRoles(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @PathVariable String userUuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.GET_ROLE_LIST,
                        teamMemberFacade.getMemberRoles(authUser, teamUuid, userUuid, page, size)
                ));
    }

    // 내 팀 프로필 및 보유 권한 (FE 팀 작업 버튼 활성화 판단용)
    @GetMapping("/members/me")
    public ResponseEntity<CommonResponse<TeamMemberResponse>> getMyMemberInfo(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.GET_MY_MEMBER_INFO,
                        teamMemberFacade.getMyMemberInfo(authUser, teamUuid)
                ));
    }

    @PatchMapping("/members/me")
    public ResponseEntity<CommonResponse<TeamMemberResponse>> updateMyProfile(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @Valid @RequestBody TeamMemberProfileRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.MEMBER_PROFILE_UPDATE,
                        teamMemberFacade.updateMyProfile(authUser, teamUuid, request)
                ));
    }

    // 팀 탈퇴
    @DeleteMapping("/members/me")
    public ResponseEntity<CommonResponse<Void>> leaveTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid
    ) {
        teamMemberFacade.leaveTeam(authUser, teamUuid);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.MEMBER_LEAVE
                ));
    }

    // 구성원 추방
    @DeleteMapping("/members/{userUuid}")
    public ResponseEntity<CommonResponse<Void>> kickMember(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @PathVariable String userUuid
    ) {
        teamMemberFacade.kickMember(authUser, teamUuid, userUuid);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.MEMBER_KICK
                ));
    }

    // 구성원 역할 교체
    @PutMapping("/members/{userUuid}/roles")
    public ResponseEntity<CommonResponse<TeamMemberResponse>> updateMemberRoles(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @PathVariable String userUuid,
            @Valid @RequestBody TeamMemberRoleRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.MEMBER_ROLE_UPDATE,
                        teamMemberFacade.updateMemberRoles(authUser, teamUuid, userUuid, request)
                ));
    }

    // 팀 소유자 위임
    @PatchMapping("/owner")
    public ResponseEntity<CommonResponse<Void>> transferOwner(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @Valid @RequestBody TeamUserRequest request
    ) {
        teamMemberFacade.transferOwner(authUser, teamUuid, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TeamSuccessCode.OWNER_TRANSFER
                ));
    }
}
