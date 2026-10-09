package com.backend.orbitflow.domain.team.dto.response;

import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.team.enums.TeamPermission;

import java.util.List;

public record TeamRoleResponse(
        Long id,
        String name,
        String color,
        int priority,
        boolean isDefault,
        List<TeamPermission> permissions
) {

    public static TeamRoleResponse from(TeamRole role) {
        return new TeamRoleResponse(
                role.getId(),
                role.getName(),
                role.getColor(),
                role.getPriority(),
                role.isDefault(),
                TeamPermission.fromMask(role.getPermissionsMask())
        );
    }
}
