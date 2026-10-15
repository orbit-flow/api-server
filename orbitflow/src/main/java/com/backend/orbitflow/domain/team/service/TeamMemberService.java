package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.team.dto.response.TeamMemberResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;
import com.backend.orbitflow.domain.team.dto.response.TeamRoleResponse;
import org.springframework.data.domain.Page;

public interface TeamMemberService {

    // 구성원 여부 확인(TeamAuthorityService.getMember)은 호출 측에서 끝낸 뒤 호출
    Page<TeamMemberResponse> getMembers(Team team, int page, int size);
    Page<TeamRoleResponse> getMemberRoles(Team team, User target, int page, int size);
    TeamMemberResponse getMyMemberInfo(Team team, TeamMember me);
    TeamMemberResponse updateMyProfile(Team team, TeamMember me, String nickname, String bio);
    void joinTeam(Team team, User user);
    // 탈퇴·추방은 호출 측이 검증 → 담당 투두 상속 → 팀 채팅방 제외 → removeMember 순서로 진행
    void checkLeavable(Team team, User me);
    TeamMember getKickTarget(Team team, User actor, User target);
    void checkKickable(int actorMask, int targetMask);
    void removeMember(TeamMember member);
    // actorMask : 요청자의 팀 권한 (호출 측이 판정해 전달, 권한 상승 방지에 사용)
    TeamMemberResponse updateMemberRoles(Team team, int actorMask, User target, List<Long> roleIds);
    void transferOwner(Team team, User owner, User newOwner);
}
