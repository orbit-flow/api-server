package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.team.repository.TeamMemberRepository;
import com.backend.orbitflow.domain.team.repository.TeamRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    // 생성자는 팀 소유자이자 첫 구성원
    public Team createTeam(User owner, String name, String icon) {
        Team team = teamRepository.save(Team.of(
                UUID.randomUUID().toString().replace("-", ""),
                name,
                icon,
                owner
        ));
        teamMemberRepository.save(TeamMember.of(team, owner));
        return team;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getMyTeams(User user) {
        return teamRepository.findMyTeams(user);
    }

    // 비소속 사용자에게는 팀 존재 여부를 노출하지 않도록 NOT_FOUND 처리
    @Transactional(readOnly = true)
    public TeamResponse getMyTeam(User user, String uuid) {
        return teamRepository.findMyTeam(user, uuid).orElseThrow(
                () -> new CommonException(TeamErrorCode.TEAM_NOT_FOUND)
        );
    }

    // TODO: 역할 구현 후 MANAGE_TEAM 권한 보유자도 수정 가능하게 확장
    public void updateTeam(User user, String uuid, String name, String icon) {
        Team team = getOwnedTeam(user, uuid);
        team.updateTeamInfo(name, icon);
    }

    // 논리적 삭제 후 30일간 보관, 모든 구성원의 소속 해제
    public void deleteTeam(User user, String uuid) {
        Team team = getOwnedTeam(user, uuid);
        team.delete();
        teamMemberRepository.deleteAllByTeam(team);
    }

    @Transactional(readOnly = true)
    public List<Team> getDeletedTeams(User owner) {
        return teamRepository.findAllByOwnerAndDeletedAtAfterOrderByDeletedAtDesc(owner, retentionThreshold());
    }

    // 소유자만 다시 소속되며, 기존 구성원은 다시 초대해야 함
    public void restoreTeam(User user, String uuid) {
        Team team = teamRepository.findByUuidAndDeletedAtAfter(uuid, retentionThreshold()).orElseThrow(
                () -> new CommonException(TeamErrorCode.TEAM_NOT_FOUND)
        );
        if (!team.isOwner(user)) {
            throw new CommonException(TeamErrorCode.TEAM_NOT_FOUND);
        }
        team.restore();
        teamMemberRepository.save(TeamMember.of(team, user));
    }

    private Team getOwnedTeam(User user, String uuid) {
        Team team = teamRepository.findByUuidAndDeletedAtIsNull(uuid).orElseThrow(
                () -> new CommonException(TeamErrorCode.TEAM_NOT_FOUND)
        );
        if (!team.isOwner(user)) {
            throw new CommonException(TeamErrorCode.NOT_TEAM_OWNER);
        }
        return team;
    }

    private LocalDateTime retentionThreshold() {
        return LocalDateTime.now().minusDays(RETENTION_DAYS);
    }
}
