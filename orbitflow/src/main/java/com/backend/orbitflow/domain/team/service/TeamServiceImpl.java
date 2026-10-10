package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.category.service.CategoryService;
import com.backend.orbitflow.domain.chat.service.ChatroomService;
import com.backend.orbitflow.domain.team.dto.response.TeamResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.team.repository.TeamInvitationRepository;
import com.backend.orbitflow.domain.team.repository.TeamMemberRepository;
import com.backend.orbitflow.domain.team.repository.TeamMemberRoleRepository;
import com.backend.orbitflow.domain.team.repository.TeamRepository;
import com.backend.orbitflow.domain.team.repository.TeamRoleRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRoleRepository teamRoleRepository;
    private final TeamMemberRoleRepository teamMemberRoleRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamAuthorityService teamAuthorityService;
    private final CategoryService categoryService;
    private final ChatroomService chatroomService;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public Team getActiveTeam(String uuid) {
        return teamRepository.findByUuidAndDeletedAtIsNull(uuid).orElseThrow(
                () -> new CommonException(TeamErrorCode.TEAM_NOT_FOUND)
        );
    }

    // 팀 행을 잠가 소유권 위임·탈퇴·추방·초대 수락·팀 삭제를 직렬화하고, 잠근 뒤의 최신 상태로 삭제 여부를 다시 확인
    // 같은 트랜잭션에서 먼저 읽은 팀이 있으면 FOR UPDATE 조회도 그 오래된 인스턴스를 반환하므로 refresh로 다시 읽음
    // 잠근 뒤 다른 행의 최신 커밋을 보도록 호출 트랜잭션은 READ_COMMITTED로 시작해야 함
    public Team lockActiveTeam(Team team) {
        entityManager.refresh(team, LockModeType.PESSIMISTIC_WRITE);
        if (team.getDeletedAt() != null) {
            throw new CommonException(TeamErrorCode.TEAM_NOT_FOUND);
        }
        return team;
    }

    // 생성자는 팀 소유자이자 첫 구성원, 초대 수락 시 부여할 기본 역할을 함께 생성
    public Team createTeam(User owner, String name, String icon) {
        Team team = teamRepository.save(Team.of(
                UUID.randomUUID().toString().replace("-", ""),
                name,
                icon,
                owner
        ));
        teamMemberRepository.save(TeamMember.of(team, owner));
        teamRoleRepository.save(TeamRole.defaultRole(team));
        return team;
    }

    @Transactional(readOnly = true)
    public Page<TeamResponse> getMyTeams(User user, int page, int size) {
        Pageable pageable = toPageable(page, size);
        return teamRepository.findMyTeams(user, pageable);
    }

    // 비소속 사용자에게는 팀 존재 여부를 노출하지 않도록 NOT_FOUND 처리
    @Transactional(readOnly = true)
    public TeamResponse getMyTeam(User user, String uuid) {
        return teamRepository.findMyTeam(user, uuid).orElseThrow(
                () -> new CommonException(TeamErrorCode.TEAM_NOT_FOUND)
        );
    }

    // 소유자 또는 MANAGE_TEAM 권한 보유자(팀 관리자)
    public void updateTeam(User user, String uuid, String name, String icon) {
        Team team = getActiveTeam(uuid);
        teamAuthorityService.checkPermission(team, user, TeamPermission.MANAGE_TEAM);
        team.updateTeamInfo(name, icon);
    }

    // 논리적 삭제 후 30일간 보관, 모든 구성원의 소속 해제 및 대기 중인 초대 취소 (역할은 복구를 위해 유지)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteTeam(User user, String uuid) {
        Team team = lockActiveTeam(getActiveTeam(uuid));
        if (!team.isOwner(user)) {
            throw new CommonException(TeamErrorCode.NOT_TEAM_OWNER);
        }
        team.delete();
        teamMemberRoleRepository.deleteAllByTeam(team);
        categoryService.deleteAllMemberPermissionsByTeam(team);
        teamMemberRepository.deleteAllByTeam(team);
        teamInvitationRepository.cancelAllPendingByTeam(team);
        chatroomService.removeTeamChatroomMembers(team);
    }

    @Transactional(readOnly = true)
    public Page<Team> getDeletedTeams(User owner, int page, int size) {
        Pageable pageable = toPageable(page, size);
        return teamRepository.findAllByOwnerAndDeletedAtAfterOrderByDeletedAtDesc(owner, retentionThreshold(), pageable);
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

    // 삭제되지 않은 팀의 소유자인지 (회원 탈퇴 가능 여부 판단용)
    @Transactional(readOnly = true)
    public boolean ownsActiveTeam(User user) {
        return teamRepository.existsByOwnerAndDeletedAtIsNull(user);
    }

    private LocalDateTime retentionThreshold() {
        return LocalDateTime.now().minusDays(RETENTION_DAYS);
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
