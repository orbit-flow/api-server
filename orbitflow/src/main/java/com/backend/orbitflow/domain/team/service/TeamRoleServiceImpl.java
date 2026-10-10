package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamRoleResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.team.repository.TeamRoleRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

// 역할 변경은 저장 즉시 적용 (권한은 요청마다 DB에서 판정하므로 별도 캐시 무효화 불필요)
@Service
@RequiredArgsConstructor
@Transactional
public class TeamRoleServiceImpl implements TeamRoleService {

    private final TeamRoleRepository teamRoleRepository;
    private final TeamAuthorityService teamAuthorityService;

    @Transactional(readOnly = true)
    public Page<TeamRoleResponse> getRoles(Team team, User me, int page, int size) {
        Pageable pageable = toPageable(page, size);
        teamAuthorityService.getMember(team, me);
        return teamRoleRepository.findAllByTeamOrderByPriorityDescIdAsc(team, pageable).map(TeamRoleResponse::from);
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
        teamRoleRepository.deleteById(role.getId());   // 역할 부여·역할 열람 권한은 DB가 연쇄 삭제
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

    // 팀의 역할 중 roleIds에 해당하는 것 (한 번에 조회)
    @Transactional(readOnly = true)
    public List<TeamRole> findRoles(Team team, Collection<Long> roleIds) {
        return roleIds.isEmpty() ? List.of() : teamRoleRepository.findAllByTeamAndIdIn(team, roleIds);
    }

    private TeamRole getRole(Team team, Long roleId) {
        return teamRoleRepository.findByIdAndTeam(roleId, team).orElseThrow(
                () -> new CommonException(TeamErrorCode.ROLE_NOT_FOUND)
        );
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
