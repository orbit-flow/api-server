package com.backend.orbitflow.domain.team.repository;

import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeamRoleRepository extends JpaRepository<TeamRole, Long> {

    Page<TeamRole> findAllByTeamOrderByPriorityDescIdAsc(Team team, Pageable pageable);

    List<TeamRole> findAllByTeamAndIdIn(Team team, Collection<Long> ids);

    Optional<TeamRole> findByIdAndTeam(Long id, Team team);

    Optional<TeamRole> findByTeamAndIsDefaultTrue(Team team);
}
