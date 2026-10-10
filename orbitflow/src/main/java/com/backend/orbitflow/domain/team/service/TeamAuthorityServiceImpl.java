package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.team.repository.TeamMemberRepository;
import com.backend.orbitflow.domain.team.repository.TeamMemberRoleRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamAuthorityServiceImpl implements TeamAuthorityService {

    private final TeamMemberRepository teamMemberRepository;
    private final TeamMemberRoleRepository teamMemberRoleRepository;

    public Optional<TeamMember> findMember(Team team, User user) {
        return teamMemberRepository.findByTeamAndUser(team, user);
    }

    // users 중 팀 구성원 (한 번에 조회)
    public List<TeamMember> findMembers(Team team, Collection<User> users) {
        return users.isEmpty() ? List.of() : teamMemberRepository.findAllByTeamAndUserIn(team, users);
    }

    // users 중 팀 구성원인 사용자 id (한 번에 조회)
    public Set<Long> findMemberUserIds(Team team, Collection<User> users) {
        if (users.isEmpty()) {
            return Set.of();
        }
        return teamMemberRepository.findAllByTeamAndUserIn(team, users).stream()
                .map(member -> member.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    // 비소속 사용자에게는 팀 존재 여부를 노출하지 않도록 NOT_FOUND 처리
    public TeamMember getMember(Team team, User user) {
        return findMember(team, user).orElseThrow(
                () -> new CommonException(TeamErrorCode.TEAM_NOT_FOUND)
        );
    }

    // 소유자는 전체 권한, 그 외에는 보유 역할의 권한을 OR 합산
    public int getPermissionMask(Team team, TeamMember member) {
        if (team.isOwner(member.getUser())) {
            return TeamPermission.all();
        }
        return teamMemberRoleRepository.findPermissionMasksByMember(member).stream()
                .reduce(0, (a, b) -> a | b);
    }

    public TeamMember checkPermission(Team team, User user, TeamPermission permission) {
        TeamMember member = getMember(team, user);
        if (!permission.isGranted(getPermissionMask(team, member))) {
            throw new CommonException(TeamErrorCode.NO_TEAM_PERMISSION);
        }
        return member;
    }

    // 권한 상승 방지 : 자신이 보유하지 않은 권한이 포함된 역할은 생성·수정·삭제·부여·회수할 수 없음
    public void checkGrantable(Team team, User actor, int mask) {
        int actorMask = getPermissionMask(team, getMember(team, actor));
        if (!TeamPermission.contains(actorMask, mask)) {
            throw new CommonException(TeamErrorCode.CANNOT_GRANT_PERMISSION);
        }
    }

    public List<User> findMemberUsers(Team team) {
        return teamMemberRepository.findAllWithUserByTeam(team).stream()
                .map(TeamMember::getUser)
                .toList();
    }

    // 팀 관리자 : 소유자 또는 MANAGE_TEAM 권한 보유자 (쿼리 1회, 구성원마다 권한을 조회하지 않음)
    public List<User> findAdminUsers(Team team) {
        return teamMemberRepository.findAdminUsers(team.getId());
    }

    // 구성원이 보유한 역할 id (구성원이 아니면 빈 집합)
    public Set<Long> findRoleIds(Team team, User user) {
        return findMember(team, user)
                .map(member -> teamMemberRoleRepository.findAllWithRoleByMember(member).stream()
                        .map(memberRole -> memberRole.getRole().getId())
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }
}
