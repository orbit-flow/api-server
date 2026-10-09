package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.category.repository.CategoryPermissionRepository;
import com.backend.orbitflow.domain.team.dto.response.TeamRoleResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.team.repository.TeamMemberRoleRepository;
import com.backend.orbitflow.domain.team.repository.TeamRoleRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 역할 변경은 저장 즉시 적용 (권한은 요청마다 DB에서 판정하므로 별도 캐시 무효화 불필요)
@Service
@RequiredArgsConstructor
@Transactional
public class TeamRoleServiceImpl implements TeamRoleService {

    private final TeamRoleRepository teamRoleRepository;
    private final TeamMemberRoleRepository teamMemberRoleRepository;
    private final TeamAuthorityService teamAuthorityService;
    private final CategoryPermissionRepository categoryPermissionRepository;

    @Transactional(readOnly = true)
    public List<TeamRoleResponse> getRoles(Team team, User me) {
        teamAuthorityService.getMember(team, me);
        return teamRoleRepository.findAllByTeamOrderByPriorityDescIdAsc(team).stream()
                .map(TeamRoleResponse::from)
                .toList();
    }

    public TeamRoleResponse createRole(Team team, User actor, String name, String color, int priority, List<TeamPermission> permissions) {
        teamAuthorityService.checkPermission(team, actor, TeamPermission.MANAGE_ROLES);
        int mask = TeamPermission.toMask(permissions);
        teamAuthorityService.checkGrantable(team, actor, mask);
        return TeamRoleResponse.from(teamRoleRepository.save(TeamRole.of(team, name, color, priority, mask)));
    }

    // 기존 권한과 새 권한 모두 요청자가 보유해야 함 (보유하지 않은 권한의 추가·회수 방지)
    public TeamRoleResponse updateRole(Team team, User actor, Long roleId, String name, String color, int priority, List<TeamPermission> permissions) {
        teamAuthorityService.checkPermission(team, actor, TeamPermission.MANAGE_ROLES);
        TeamRole role = getRole(team, roleId);
        int mask = TeamPermission.toMask(permissions);
        teamAuthorityService.checkGrantable(team, actor, role.getPermissionsMask() | mask);
        role.updateRole(name, color, priority, mask);
        return TeamRoleResponse.from(role);
    }

    public void deleteRole(Team team, User actor, Long roleId) {
        teamAuthorityService.checkPermission(team, actor, TeamPermission.MANAGE_ROLES);
        TeamRole role = getRole(team, roleId);
        if (role.isDefault()) {
            throw new CommonException(TeamErrorCode.DEFAULT_ROLE_DELETE);
        }
        teamAuthorityService.checkGrantable(team, actor, role.getPermissionsMask());
        teamMemberRoleRepository.deleteAllByRole(role);
        categoryPermissionRepository.deleteAllByRole(role);
        teamRoleRepository.deleteById(role.getId());
    }

    // 팀당 기본 역할은 1개, 이후 초대 수락자부터 적용 (기존 구성원의 역할은 변경하지 않음)
    public TeamRoleResponse updateDefaultRole(Team team, User actor, Long roleId) {
        teamAuthorityService.checkPermission(team, actor, TeamPermission.MANAGE_ROLES);
        TeamRole role = getRole(team, roleId);
        teamAuthorityService.checkGrantable(team, actor, role.getPermissionsMask());
        teamRoleRepository.findByTeamAndIsDefaultTrue(team)
                .ifPresent(previous -> previous.updateDefault(false));
        role.updateDefault(true);
        return TeamRoleResponse.from(role);
    }

    private TeamRole getRole(Team team, Long roleId) {
        return teamRoleRepository.findByIdAndTeam(roleId, team).orElseThrow(
                () -> new CommonException(TeamErrorCode.ROLE_NOT_FOUND)
        );
    }
}
