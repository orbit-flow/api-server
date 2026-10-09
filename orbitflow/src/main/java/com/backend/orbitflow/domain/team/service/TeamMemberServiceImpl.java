package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.category.repository.CategoryPermissionRepository;
import com.backend.orbitflow.domain.team.dto.response.TeamMemberResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.entity.TeamMemberRole;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.team.repository.TeamMemberRepository;
import com.backend.orbitflow.domain.team.repository.TeamMemberRoleRepository;
import com.backend.orbitflow.domain.team.repository.TeamRoleRepository;
import com.backend.orbitflow.domain.todo.repository.TodoRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.domain.notification.event.TeamJoinedEvent;
import com.backend.orbitflow.domain.notification.event.TeamLeftEvent;
import com.backend.orbitflow.domain.notification.event.TeamRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamMemberServiceImpl implements TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;
    private final TeamRoleRepository teamRoleRepository;
    private final TeamMemberRoleRepository teamMemberRoleRepository;
    private final TeamAuthorityService teamAuthorityService;
    private final CategoryPermissionRepository categoryPermissionRepository;
    private final TodoRepository todoRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getMembers(Team team, User me) {
        teamAuthorityService.getMember(team, me);
        List<TeamMember> members = teamMemberRepository.findAllWithUserByTeam(team);
        Map<Long, List<TeamRole>> rolesByMember = teamMemberRoleRepository.findAllWithRoleByMemberIn(members).stream()
                .collect(Collectors.groupingBy(
                        memberRole -> memberRole.getMember().getId(),
                        Collectors.mapping(TeamMemberRole::getRole, Collectors.toList())
                ));
        return members.stream()
                .map(member -> toResponse(team, member, rolesByMember.getOrDefault(member.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamMemberResponse getMyMemberInfo(Team team, User me) {
        return toResponse(team, teamAuthorityService.getMember(team, me));
    }

    public TeamMemberResponse updateMyProfile(Team team, User me, String nickname, String bio) {
        TeamMember member = teamAuthorityService.getMember(team, me);
        member.updateProfile(nickname, bio);
        return toResponse(team, member);
    }

    // 초대 수락 시점에만 호출, 기본 역할을 자동 부여
    public void joinTeam(Team team, User user) {
        if (teamMemberRepository.existsByTeamAndUser(team, user)) {
            throw new CommonException(TeamErrorCode.ALREADY_TEAM_MEMBER);
        }
        TeamMember member = teamMemberRepository.save(TeamMember.of(team, user));
        teamMemberRoleRepository.save(TeamMemberRole.of(member, getOrCreateDefaultRole(team)));
        eventPublisher.publishEvent(new TeamJoinedEvent(team.getId(), user.getId()));
    }

    // 탈퇴 즉시 팀 카테고리·투두 접근 불가, 소유자는 위임 후에만 탈퇴 가능
    public void leaveTeam(Team team, User me) {
        if (team.isOwner(me)) {
            throw new CommonException(TeamErrorCode.OWNER_CANNOT_LEAVE);
        }
        removeMember(team, teamAuthorityService.getMember(team, me));
        eventPublisher.publishEvent(new TeamLeftEvent(team.getId(), me.getId(), false));
    }

    // 소유자가 아니면 자신이 보유하지 않은 권한을 가진 구성원은 추방할 수 없음
    public void kickMember(Team team, User actor, User target) {
        teamAuthorityService.checkPermission(team, actor, TeamPermission.MANAGE_MEMBERS);
        if (actor.getId().equals(target.getId())) {
            throw new CommonException(TeamErrorCode.SELF_KICK);
        }
        if (team.isOwner(target)) {
            throw new CommonException(TeamErrorCode.CANNOT_KICK_OWNER);
        }
        TeamMember targetMember = getTargetMember(team, target);
        teamAuthorityService.checkGrantable(team, actor, teamAuthorityService.getPermissionMask(team, targetMember));
        removeMember(team, targetMember);
        eventPublisher.publishEvent(new TeamLeftEvent(team.getId(), target.getId(), true));
    }

    // 요청한 역할 목록으로 교체, 추가·제거되는 역할의 권한은 모두 요청자가 보유해야 함
    public TeamMemberResponse updateMemberRoles(Team team, User actor, User target, List<Long> roleIds) {
        teamAuthorityService.checkPermission(team, actor, TeamPermission.MANAGE_ROLES);
        TeamMember targetMember = getTargetMember(team, target);

        Set<Long> requestedIds = new HashSet<>(roleIds);
        List<TeamRole> requestedRoles = teamRoleRepository.findAllByTeamAndIdIn(team, requestedIds);
        if (requestedRoles.size() != requestedIds.size()) {
            throw new CommonException(TeamErrorCode.ROLE_NOT_FOUND);
        }

        List<TeamMemberRole> currentRoles = teamMemberRoleRepository.findAllWithRoleByMember(targetMember);
        Set<Long> currentIds = currentRoles.stream()
                .map(memberRole -> memberRole.getRole().getId())
                .collect(Collectors.toSet());

        List<TeamMemberRole> removed = currentRoles.stream()
                .filter(memberRole -> !requestedIds.contains(memberRole.getRole().getId()))
                .toList();
        List<TeamRole> added = requestedRoles.stream()
                .filter(role -> !currentIds.contains(role.getId()))
                .toList();

        int changedMask = 0;
        for (TeamMemberRole memberRole : removed) {
            changedMask |= memberRole.getRole().getPermissionsMask();
        }
        for (TeamRole role : added) {
            changedMask |= role.getPermissionsMask();
        }
        teamAuthorityService.checkGrantable(team, actor, changedMask);

        teamMemberRoleRepository.deleteAll(removed);
        teamMemberRoleRepository.saveAll(added.stream()
                .map(role -> TeamMemberRole.of(targetMember, role))
                .toList());
        if (!removed.isEmpty() || !added.isEmpty()) {
            eventPublisher.publishEvent(new TeamRoleChangedEvent(team.getId(), target.getId()));
        }
        return toResponse(team, targetMember, requestedRoles);
    }

    public void transferOwner(Team team, User owner, User newOwner) {
        if (!team.isOwner(owner)) {
            throw new CommonException(TeamErrorCode.NOT_TEAM_OWNER);
        }
        if (owner.getId().equals(newOwner.getId())) {
            throw new CommonException(TeamErrorCode.ALREADY_TEAM_OWNER);
        }
        getTargetMember(team, newOwner);
        team.transferOwner(newOwner);
    }

    private TeamMember getTargetMember(Team team, User target) {
        return teamMemberRepository.findByTeamAndUser(team, target).orElseThrow(
                () -> new CommonException(TeamErrorCode.MEMBER_NOT_FOUND)
        );
    }

    // 담당하던 미완료 팀 투두는 상위 권한(전체 권한)을 가진 팀 소유자가 상속
    private void removeMember(Team team, TeamMember member) {
        todoRepository.reassignIncompleteTeamTodos(team, member.getUser(), team.getOwner());
        teamMemberRoleRepository.deleteAllByMember(member);
        categoryPermissionRepository.deleteAllByMember(member);
        teamMemberRepository.deleteById(member.getId());
    }

    // 역할 기능 이전에 생성된 팀은 기본 역할이 없으므로 최초 사용 시 생성
    private TeamRole getOrCreateDefaultRole(Team team) {
        return teamRoleRepository.findByTeamAndIsDefaultTrue(team)
                .orElseGet(() -> teamRoleRepository.save(TeamRole.defaultRole(team)));
    }

    private TeamMemberResponse toResponse(Team team, TeamMember member) {
        List<TeamRole> roles = teamMemberRoleRepository.findAllWithRoleByMember(member).stream()
                .map(TeamMemberRole::getRole)
                .toList();
        return toResponse(team, member, roles);
    }

    private TeamMemberResponse toResponse(Team team, TeamMember member, List<TeamRole> roles) {
        boolean owner = team.isOwner(member.getUser());
        int mask = owner ? TeamPermission.all() : roles.stream()
                .mapToInt(TeamRole::getPermissionsMask)
                .reduce(0, (a, b) -> a | b);
        return TeamMemberResponse.of(member, owner, roles, mask);
    }
}
