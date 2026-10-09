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

import java.util.Collection;
import java.util.Optional;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    boolean existsByReporterAndContentTypeAndReportedIdAndStatusIn(
            User reporter, ReportContentType contentType, Long reportedId, Collection<ReportStatus> statuses
    );

    Page<UserReport> findAllByReporterOrderByCreatedAtDesc(User reporter, Pageable pageable);

    @Query("select r from UserReport r join fetch r.reporter where r.id = :id")
    Optional<UserReport> findWithReporterById(@Param("id") Long id);

    // 관리자 목록 : 상태·유형 필터 (null이면 전체)
    @Query(value = """
            select r from UserReport r
            join fetch r.reporter
            where (:status is null or r.status = :status)
              and (:contentType is null or r.contentType = :contentType)
            order by r.createdAt desc
            """,
            countQuery = """
            select count(r) from UserReport r
            where (:status is null or r.status = :status)
              and (:contentType is null or r.contentType = :contentType)
            """)
    Page<UserReport> search(
            @Param("status") ReportStatus status,
            @Param("contentType") ReportContentType contentType,
            Pageable pageable
    );
}
