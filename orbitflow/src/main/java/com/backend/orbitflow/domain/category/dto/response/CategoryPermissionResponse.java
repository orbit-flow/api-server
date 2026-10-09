package com.backend.orbitflow.domain.category.dto.response;

import com.backend.orbitflow.domain.category.entity.CategoryPermission;

import java.util.List;
import java.util.Objects;

public record CategoryPermissionResponse(
        Long categoryId,
        List<RoleInfo> roles,
        List<MemberInfo> members
) {

    public record RoleInfo(Long id, String name, String color) {
    }

    // uuid : 사용자 uuid
    public record MemberInfo(String uuid, String name, String nickname) {
    }

    public static CategoryPermissionResponse of(Long categoryId, List<CategoryPermission> permissions) {
        return new CategoryPermissionResponse(
                categoryId,
                permissions.stream()
                        .map(CategoryPermission::getRole)
                        .filter(Objects::nonNull)
                        .map(role -> new RoleInfo(role.getId(), role.getName(), role.getColor()))
                        .toList(),
                permissions.stream()
                        .map(CategoryPermission::getMember)
                        .filter(Objects::nonNull)
                        .map(member -> new MemberInfo(
                                member.getUser().getUuid(),
                                member.getUser().getName(),
                                member.getNickname()
                        ))
                        .toList()
        );
    }
}
