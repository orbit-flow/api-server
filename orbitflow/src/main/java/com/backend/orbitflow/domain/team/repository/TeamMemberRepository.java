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
import com.backend.orbitflow.domain.team.dto.TeamMembershipRow;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeamAndUser(Team team, User user);

    boolean existsByTeamAndUser(Team team, User user);

    @Query("select m from TeamMember m join fetch m.user where m.team = :team order by m.createdAt asc")
    List<TeamMember> findAllWithUserByTeam(@Param("team") Team team);

    @Query("""
            select new com.backend.orbitflow.domain.team.dto.TeamMembershipRow(t.id, t.owner.id)
            from TeamMember m
            join m.team t
            where m.user = :user
              and t.deletedAt is null
            """)
    List<TeamMembershipRow> findMembershipsByUser(@Param("user") User user);

    // 팀 삭제 시 모든 구성원 소속 해제
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TeamMember m where m.team = :team")
    void deleteAllByTeam(@Param("team") Team team);
}
