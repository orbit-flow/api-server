package com.backend.orbitflow.domain.team.repository;

import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeamAndUser(Team team, User user);
    List<TeamMember> findAllByTeamAndUserIn(Team team, Collection<User> users);

    boolean existsByTeamAndUser(Team team, User user);

    @Query("select m from TeamMember m join fetch m.user where m.team = :team order by m.createdAt asc")
    List<TeamMember> findAllWithUserByTeam(@Param("team") Team team);

    // 구성원 목록 페이지 (가입 순)
    @Query(value = "select m from TeamMember m join fetch m.user where m.team = :team order by m.createdAt asc, m.id asc",
            countQuery = "select count(m) from TeamMember m where m.team = :team")
    Page<TeamMember> findPageWithUserByTeam(@Param("team") Team team, Pageable pageable);

    // 팀 관리자 : 소유자 또는 팀 관리 권한(비트 1) 역할 보유자
    @Query(nativeQuery = true, value = """
            select u.* from users u
            join team_members m on m.user_id = u.id
            join teams t on t.id = m.team_id
            where m.team_id = :teamId
              and (t.owner_id = u.id
                   or exists (select 1 from team_member_roles mr
                              join team_roles r on r.id = mr.role_id
                              where mr.member_id = m.id and (r.permissions_mask & 1) <> 0))
            """)
    List<User> findAdminUsers(@Param("teamId") Long teamId);

    // 팀 삭제 시 모든 구성원 소속 해제
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TeamMember m where m.team = :team")
    void deleteAllByTeam(@Param("team") Team team);
}
