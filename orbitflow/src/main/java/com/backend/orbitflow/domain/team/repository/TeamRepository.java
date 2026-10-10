package com.backend.orbitflow.domain.team.repository;

import com.backend.orbitflow.domain.team.dto.response.TeamResponse;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByUuidAndDeletedAtIsNull(String uuid);

    boolean existsByOwnerAndDeletedAtIsNull(User owner);

    // 복구 가능 기간(deletedAt > threshold) 내의 삭제된 팀
    Optional<Team> findByUuidAndDeletedAtAfter(String uuid, LocalDateTime threshold);

    Page<Team> findAllByOwnerAndDeletedAtAfterOrderByDeletedAtDesc(User owner, LocalDateTime threshold, Pageable pageable);

    // user가 소속된 팀 목록
    @Query(value = """
            select new com.backend.orbitflow.domain.team.dto.response.TeamResponse(
                t.uuid, t.name, t.icon, o.name,
                (select count(m2) from TeamMember m2 where m2.team = t),
                t.createdAt)
            from TeamMember m
            join m.team t
            join t.owner o
            where m.user = :user
              and t.deletedAt is null
            order by t.createdAt desc
            """,
            countQuery = "select count(m) from TeamMember m where m.user = :user and m.team.deletedAt is null")
    Page<TeamResponse> findMyTeams(@Param("user") User user, Pageable pageable);

    // user가 소속된 팀만 조회 (비소속이면 empty)
    @Query("""
            select new com.backend.orbitflow.domain.team.dto.response.TeamResponse(
                t.uuid, t.name, t.icon, o.name,
                (select count(m2) from TeamMember m2 where m2.team = t),
                t.createdAt)
            from TeamMember m
            join m.team t
            join t.owner o
            where m.user = :user
              and t.uuid = :uuid
              and t.deletedAt is null
            """)
    Optional<TeamResponse> findMyTeam(@Param("user") User user, @Param("uuid") String uuid);
}
