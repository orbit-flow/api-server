package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.Set;

public interface TeamAuthorityService {

    Optional<TeamMember> findMember(Team team, User user);
    Set<Long> findMemberUserIds(Team team, Collection<User> users);
    List<TeamMember> findMembers(Team team, Collection<User> users);
    TeamMember getMember(Team team, User user);
    int getPermissionMask(Team team, TeamMember member);
    TeamMember checkPermission(Team team, User user, TeamPermission permission);
    void checkGrantable(Team team, User actor, int mask);

    // 알림 대상 조회용
    List<User> findMemberUsers(Team team);
    List<User> findAdminUsers(Team team);
    Set<Long> findRoleIds(Team team, User user);
}
