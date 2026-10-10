package com.backend.orbitflow.domain.team.repository;

import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamInvitation;
import com.backend.orbitflow.domain.team.enums.TeamInvitationStatus;
import com.backend.orbitflow.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    @Query("""
            select i from TeamInvitation i
            join fetch i.team
            join fetch i.inviter
            join fetch i.invitee
            where i.uuid = :uuid
            """)
    Optional<TeamInvitation> findWithAllByUuid(@Param("uuid") String uuid);

    // 같은 초대의 동시 수락·거절·취소를 직렬화 (join fetch 없이 team_invitations 행만 잠금)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from TeamInvitation i where i.uuid = :uuid")
    Optional<TeamInvitation> findByUuidForUpdate(@Param("uuid") String uuid);

    // 같은 팀의 동시 초대 생성을 직렬화 (대기 중 초대 중복 확인과 저장 사이의 경합 방지, teams 행만 잠금)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Team t where t.id = :teamId")
    Optional<Team> findTeamForUpdate(@Param("teamId") Long teamId);

    // 만료되지 않은(createdAt이 cutoff 이후) 대기 중인 초대 여부
    boolean existsByTeamAndInviteeAndStatusAndCreatedAtAfter(Team team, User invitee, TeamInvitationStatus status, LocalDateTime cutoff);

    // 팀이 보낸 대기 중인 초대 목록 (만료된 초대 제외)
    @Query(value = """
            select i from TeamInvitation i
            join fetch i.team
            join fetch i.inviter
            join fetch i.invitee
            where i.team = :team
              and i.status = com.backend.orbitflow.domain.team.enums.TeamInvitationStatus.PENDING
              and i.createdAt > :cutoff
            order by i.createdAt desc
            """,
            countQuery = """
            select count(i) from TeamInvitation i
            where i.team = :team
              and i.status = com.backend.orbitflow.domain.team.enums.TeamInvitationStatus.PENDING
              and i.createdAt > :cutoff
            """)
    Page<TeamInvitation> findPendingByTeam(@Param("team") Team team, @Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    // 내가 받은 대기 중인 초대 목록 (삭제된 팀·만료된 초대 제외)
    @Query(value = """
            select i from TeamInvitation i
            join fetch i.team t
            join fetch i.inviter
            join fetch i.invitee
            where i.invitee = :invitee
              and i.status = com.backend.orbitflow.domain.team.enums.TeamInvitationStatus.PENDING
              and t.deletedAt is null
              and i.createdAt > :cutoff
            order by i.createdAt desc
            """,
            countQuery = """
            select count(i) from TeamInvitation i
            where i.invitee = :invitee
              and i.status = com.backend.orbitflow.domain.team.enums.TeamInvitationStatus.PENDING
              and i.team.deletedAt is null
              and i.createdAt > :cutoff
            """)
    Page<TeamInvitation> findPendingByInvitee(@Param("invitee") User invitee, @Param("cutoff") LocalDateTime cutoff, Pageable pageable);

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
