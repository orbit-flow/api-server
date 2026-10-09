package com.backend.orbitflow.domain.team.repository;

import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.entity.TeamMemberRole;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TeamMemberRoleRepository extends JpaRepository<TeamMemberRole, Long> {

    @Query("select r.permissionsMask from TeamMemberRole mr join mr.role r where mr.member = :member")
    List<Integer> findPermissionMasksByMember(@Param("member") TeamMember member);

    @Query("select mr from TeamMemberRole mr join fetch mr.role where mr.member = :member")
    List<TeamMemberRole> findAllWithRoleByMember(@Param("member") TeamMember member);

    @Query("select mr from TeamMemberRole mr join fetch mr.role where mr.member in :members")
    List<TeamMemberRole> findAllWithRoleByMemberIn(@Param("members") Collection<TeamMember> members);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TeamMemberRole mr where mr.member = :member")
    void deleteAllByMember(@Param("member") TeamMember member);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TeamMemberRole mr where mr.role = :role")
    void deleteAllByRole(@Param("role") TeamRole role);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TeamMemberRole mr where mr.member in (select m from TeamMember m where m.team = :team)")
    void deleteAllByTeam(@Param("team") Team team);
}
