package com.backend.orbitflow.domain.report.entity;

import com.backend.orbitflow.domain.report.enums.ReportContentType;
import com.backend.orbitflow.domain.report.enums.ReportStatus;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_reports")
public class UserReport extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // 신고 대상 id, 대상 유형은 contentType으로 판별
    @Column(name = "reported_id", nullable = false)
    private Long reportedId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ReportContentType contentType;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    public static UserReport of(User reporter, Long reportedId, ReportContentType contentType, String reason) {
        return new UserReport(
                null, reporter, reportedId, contentType, reason, ReportStatus.RECEIVED
        );
    }

    public void updateStatus(ReportStatus status) {
        this.status = status;
    }
}
