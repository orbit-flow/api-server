package com.backend.orbitflow.domain.team.dto.response;

import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// uuid : 사용자 uuid, permissions : 소유자면 전체, 아니면 역할 권한 합산 (FE 버튼 활성화 판단용)
public record TeamMemberResponse(
        String uuid,
        String name,
        String profileImage,
        String nickname,
        String bio,
        boolean owner,
        List<RoleInfo> roles,
        List<TeamPermission> permissions,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime joinedAt
) {

    public record RoleInfo(
            Long id,
            String name,
            String color
    ) {
    }

    public static TeamMemberResponse of(TeamMember member, boolean owner, List<TeamRole> roles, int permissionsMask) {
        User user = member.getUser();
        return new TeamMemberResponse(
                user.getUuid(),
                user.getName(),
                user.getProfileImage(),
                member.getNickname(),
                member.getBio(),
                owner,
                roles.stream()
                        .sorted(Comparator.comparingInt(TeamRole::getPriority).reversed())
                        .map(role -> new RoleInfo(role.getId(), role.getName(), role.getColor()))
                        .toList(),
                TeamPermission.fromMask(permissionsMask),
                member.getCreatedAt()
        );
    }
}
