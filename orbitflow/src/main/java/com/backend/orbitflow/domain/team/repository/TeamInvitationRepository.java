package com.backend.orbitflow.domain.team.repository;

import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamInvitation;
import com.backend.orbitflow.domain.team.enums.TeamInvitationStatus;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    @Query("""
            select i from TeamInvitation i
            join fetch i.team
            join fetch i.inviter
            join fetch i.invitee
            where i.uuid = :uuid
            """)
    Optional<TeamInvitation> findWithAllByUuid(@Param("uuid") String uuid);

    boolean existsByTeamAndInviteeAndStatus(Team team, User invitee, TeamInvitationStatus status);

    // 팀이 보낸 대기 중인 초대 목록
    @Query("""
            select i from TeamInvitation i
            join fetch i.team
            join fetch i.inviter
            join fetch i.invitee
            where i.team = :team
              and i.status = com.backend.orbitflow.domain.team.enums.TeamInvitationStatus.PENDING
            order by i.createdAt desc
            """)
    List<TeamInvitation> findPendingByTeam(@Param("team") Team team);

    // 내가 받은 대기 중인 초대 목록 (삭제된 팀 제외)
    @Query("""
            select i from TeamInvitation i
            join fetch i.team t
            join fetch i.inviter
            join fetch i.invitee
            where i.invitee = :invitee
              and i.status = com.backend.orbitflow.domain.team.enums.TeamInvitationStatus.PENDING
              and t.deletedAt is null
            order by i.createdAt desc
            """)
    List<TeamInvitation> findPendingByInvitee(@Param("invitee") User invitee);

    // 팀 삭제 시 대기 중인 초대 일괄 취소
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update TeamInvitation i
            set i.status = com.backend.orbitflow.domain.team.enums.TeamInvitationStatus.CANCELLED
            where i.team = :team
              and i.status = com.backend.orbitflow.domain.team.enums.TeamInvitationStatus.PENDING
            """)
    void cancelAllPendingByTeam(@Param("team") Team team);
}
