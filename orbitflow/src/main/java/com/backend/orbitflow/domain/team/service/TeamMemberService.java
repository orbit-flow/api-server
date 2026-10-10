package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamMemberResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;
import java.util.Map;
import com.backend.orbitflow.domain.team.dto.response.TeamRoleResponse;
import org.springframework.data.domain.Page;

public interface TeamMemberService {

    Page<TeamMemberResponse> getMembers(Team team, User me, int page, int size);
    Page<TeamRoleResponse> getMemberRoles(Team team, User me, User target, int page, int size);
    TeamMemberResponse getMyMemberInfo(Team team, User me);
    TeamMemberResponse updateMyProfile(Team team, User me, String nickname, String bio);
    void joinTeam(Team team, User user);
    // 탈퇴·추방 : 담당하던 미완료 팀 투두를 상속받은 구성원 id → 투두 수 반환
    Map<Long, Integer> leaveTeam(Team team, User me);
    Map<Long, Integer> kickMember(Team team, User actor, User target);
    TeamMemberResponse updateMemberRoles(Team team, User actor, User target, List<Long> roleIds);
    void transferOwner(Team team, User owner, User newOwner);
}
