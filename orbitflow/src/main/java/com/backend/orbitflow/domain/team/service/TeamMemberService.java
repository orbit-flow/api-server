package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamMemberResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;

public interface TeamMemberService {

    List<TeamMemberResponse> getMembers(Team team, User me);
    TeamMemberResponse getMyMemberInfo(Team team, User me);
    TeamMemberResponse updateMyProfile(Team team, User me, String nickname, String bio);
    void joinTeam(Team team, User user);
    void leaveTeam(Team team, User me);
    void kickMember(Team team, User actor, User target);
    TeamMemberResponse updateMemberRoles(Team team, User actor, User target, List<Long> roleIds);
    void transferOwner(Team team, User owner, User newOwner);
}
