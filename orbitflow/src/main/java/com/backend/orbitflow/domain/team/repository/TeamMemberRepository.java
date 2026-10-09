package com.backend.orbitflow.domain.team.repository;

import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    // 팀 삭제 시 모든 구성원 소속 해제
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TeamMember m where m.team = :team")
    void deleteAllByTeam(@Param("team") Team team);
}
