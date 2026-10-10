package com.backend.orbitflow.domain.report.repository;

import com.backend.orbitflow.domain.report.entity.UserReport;
import com.backend.orbitflow.domain.report.enums.ReportContentType;
import com.backend.orbitflow.domain.report.enums.ReportStatus;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import com.backend.orbitflow.domain.report.dto.response.ReportListResponse;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    boolean existsByReporterAndContentTypeAndReportedIdAndStatusIn(
            User reporter, ReportContentType contentType, Long reportedId, Collection<ReportStatus> statuses
    );

    // 신고자의 특정 시각 이후 접수 건수 (하루 신고 횟수 제한용)
    long countByReporterAndCreatedAtGreaterThanEqual(User reporter, LocalDateTime from);

    // 내 신고 목록 : 신고 대상(게시글·댓글·사용자)과 작성자까지 한 번에 조회해 목록 응답으로 바로 반환 (N+1 방지)
    // 대상 요약은 신고 당시 스냅샷 : 이후 비공개 전환·차단된 대상의 현재 원문을 신고자에게 노출하지 않음
    @Query(value = """
            select new com.backend.orbitflow.domain.report.dto.response.ReportListResponse(
                r.id,
                r.contentType,
                case when r.contentType = com.backend.orbitflow.domain.report.enums.ReportContentType.USER then null else r.reportedId end,
                case r.contentType
                    when com.backend.orbitflow.domain.report.enums.ReportContentType.POST then pu.uuid
                    when com.backend.orbitflow.domain.report.enums.ReportContentType.COMMENT then cu.uuid
                    when com.backend.orbitflow.domain.report.enums.ReportContentType.USER then tu.uuid
                end,
                r.targetSnapshot,
                case
                    when p.id is not null or c.id is not null then true
                    when tu.id is not null and tu.deletedAt is null then true
                    else false
                end,
                r.reason, r.status, rp.uuid, rp.name, r.createdAt, r.updatedAt)
            from UserReport r
            join r.reporter rp
            left join Post p on r.contentType = com.backend.orbitflow.domain.report.enums.ReportContentType.POST and p.id = r.reportedId
            left join p.user pu
            left join Comment c on r.contentType = com.backend.orbitflow.domain.report.enums.ReportContentType.COMMENT and c.id = r.reportedId
            left join c.user cu
            left join User tu on r.contentType = com.backend.orbitflow.domain.report.enums.ReportContentType.USER and tu.id = r.reportedId
            where r.reporter = :reporter
            order by r.createdAt desc
            """,
            countQuery = "select count(r) from UserReport r where r.reporter = :reporter")
    Page<ReportListResponse> findListByReporter(@Param("reporter") User reporter, Pageable pageable);

    @Query("select r from UserReport r join fetch r.reporter where r.id = :id")
    Optional<UserReport> findWithReporterById(@Param("id") Long id);

    // 관리자 목록 : 상태·유형 필터 (null이면 전체), 목록 응답으로 바로 반환
    // 원문은 요약 길이(50자) + 말줄임 판정용 1자만 읽음 (TEXT 전체를 전송하지 않도록)
    @Query(value = """
            select new com.backend.orbitflow.domain.report.dto.response.ReportListResponse(
                r.id,
                r.contentType,
                case when r.contentType = com.backend.orbitflow.domain.report.enums.ReportContentType.USER then null else r.reportedId end,
                case r.contentType
                    when com.backend.orbitflow.domain.report.enums.ReportContentType.POST then pu.uuid
                    when com.backend.orbitflow.domain.report.enums.ReportContentType.COMMENT then cu.uuid
                    when com.backend.orbitflow.domain.report.enums.ReportContentType.USER then tu.uuid
                end,
                case r.contentType
                    when com.backend.orbitflow.domain.report.enums.ReportContentType.POST then substring(p.content, 1, 51)
                    when com.backend.orbitflow.domain.report.enums.ReportContentType.COMMENT then substring(c.content, 1, 51)
                    when com.backend.orbitflow.domain.report.enums.ReportContentType.USER then tu.name
                end,
                case
                    when p.id is not null or c.id is not null then true
                    when tu.id is not null and tu.deletedAt is null then true
                    else false
                end,
                r.reason, r.status, rp.uuid, rp.name, r.createdAt, r.updatedAt)
            from UserReport r
            join r.reporter rp
            left join Post p on r.contentType = com.backend.orbitflow.domain.report.enums.ReportContentType.POST and p.id = r.reportedId
            left join p.user pu
            left join Comment c on r.contentType = com.backend.orbitflow.domain.report.enums.ReportContentType.COMMENT and c.id = r.reportedId
            left join c.user cu
            left join User tu on r.contentType = com.backend.orbitflow.domain.report.enums.ReportContentType.USER and tu.id = r.reportedId
            where (:status is null or r.status = :status)
              and (:contentType is null or r.contentType = :contentType)
            order by r.createdAt desc
            """,
            countQuery = """
            select count(r) from UserReport r
            where (:status is null or r.status = :status)
              and (:contentType is null or r.contentType = :contentType)
            """)
    Page<ReportListResponse> search(
            @Param("status") ReportStatus status,
            @Param("contentType") ReportContentType contentType,
            Pageable pageable
    );
}
