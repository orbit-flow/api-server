package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.Map;
import java.util.Set;

// 다른 도메인 서비스를 호출하지 않는 순수 정책 : 판정에 필요한 차단·팔로우·팀 구성원 정보는 호출 측(Facade)이 조회해서 전달
// teamMask : 팀 구성원의 권한 mask, 구성원이 아니면 null
public interface CategoryAuthorityService {

    // 개인 카테고리 열람 가능 여부 (blocked : 소유자와 차단 관계, followState : 조회자 → 소유자 팔로우 상태)
    boolean canViewPersonal(Category category, User viewer, boolean blocked, FollowState followState);
    // 팀 카테고리 열람 가능 여부 (member : 조회자의 팀 구성원 정보, 구성원이 아니면 null)
    boolean canViewTeam(Category category, TeamMember member, Integer teamMask);
    void checkView(boolean viewable);
    void checkEdit(Category category, User actor, Integer teamMask);
    // 팀 카테고리 관리(MANAGE_CATEGORIES) 권한 확인
    void checkManage(Integer teamMask);

    // 알림 대상 조회용 : 팔로워 조회는 호출 측(CategoryFacade.findNotifiableViewers)이 FOLLOWER_CHUNK 단위로 반복하며 묶음마다 filterNotifiableViewers 적용
    int FOLLOWER_CHUNK = 500;

    Set<Long> findViewerUserIds(Category teamCategory);
    // 팔로워에게 알림을 보낼 수 있는 카테고리인지 (개인 PRIVATE은 팔로워도 볼 수 없음)
    boolean hasNotifiableFollowers(Category category);
    // 팔로워 중 카테고리를 볼 수 있는 사용자 id (조회 권한 쿼리 1회), null이면 팔로워 전원이 조회 가능
    Set<Long> findNotifiableViewerIds(Category category);
    // 팔로워 묶음(id → uuid)에서 viewerIds에 속한 사용자만 (viewerIds가 null이면 전체)
    Map<Long, String> filterNotifiableViewers(Map<Long, String> followers, Set<Long> viewerIds);
}
