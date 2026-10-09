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

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamAuthorityServiceImpl implements TeamAuthorityService {

    private final TeamMemberRepository teamMemberRepository;
    private final TeamMemberRoleRepository teamMemberRoleRepository;

    public Optional<TeamMember> findMember(Team team, User user) {
        return teamMemberRepository.findByTeamAndUser(team, user);
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
}
