package com.backend.orbitflow.domain.team.dto.response;

import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// uuid : 사용자 uuid, permissions : 소유자면 전체, 아니면 역할 권한 합산 (FE 버튼 활성화 판단용, 권한 종류는 고정 6개)
// 역할은 개수가 정해져 있지 않으므로 대표 역할(우선순위가 가장 높은 역할)과 개수만 포함
// 전체 역할은 GET /api/teams/{teamUuid}/members/{userUuid}/roles 로 페이지 조회
public record TeamMemberResponse(
        String uuid,
        String name,
        String profileImage,
        String nickname,
        String bio,
        boolean owner,
        List<TeamPermission> permissions,
        int roleCount,
        Long topRoleId,
        String topRoleName,
        String topRoleColor,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime joinedAt
) {

    public static TeamMemberResponse of(TeamMember member, boolean owner, List<TeamRole> roles, int permissionsMask) {
        User user = member.getUser();
        Optional<TeamRole> top = roles.stream()
                .max(Comparator.comparingInt(TeamRole::getPriority).thenComparing(TeamRole::getId, Comparator.reverseOrder()));
        return new TeamMemberResponse(
                user.getUuid(),
                user.getName(),
                user.getProfileImage(),
                member.getNickname(),
                member.getBio(),
                owner,
                TeamPermission.fromMask(permissionsMask),
                roles.size(),
                top.map(TeamRole::getId).orElse(null),
                top.map(TeamRole::getName).orElse(null),
                top.map(TeamRole::getColor).orElse(null),
                member.getCreatedAt()
        );
    }
}
