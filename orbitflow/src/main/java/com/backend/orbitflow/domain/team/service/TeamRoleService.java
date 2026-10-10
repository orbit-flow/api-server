package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamRoleResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.team.enums.TeamPermission;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;

public interface TeamRoleService {

    Page<TeamRoleResponse> getRoles(Team team, int page, int size);
    // actorMask : 요청자의 팀 권한 (호출 측이 판정해 전달, 권한 상승 방지에 사용)
    TeamRoleResponse createRole(Team team, int actorMask, String name, String color, int priority, List<TeamPermission> permissions);
    TeamRoleResponse updateRole(Team team, int actorMask, Long roleId, String name, String color, int priority, List<TeamPermission> permissions);
    void deleteRole(Team team, int actorMask, Long roleId);
    TeamRoleResponse updateDefaultRole(Team team, int actorMask, Long roleId);
    List<TeamRole> findRoles(Team team, Collection<Long> roleIds);
}
