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

    // 신고 시점의 대상 작성자 (USER 유형은 피신고자), 콘텐츠가 삭제되어도 제재 대상을 특정하기 위해 보관
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    // 신고 시점의 대상 내용 (게시글·댓글 본문, 사용자 이름), 콘텐츠가 삭제되어도 증거로 남김
    @Column(name = "target_snapshot", length = 500)
    private String targetSnapshot;

    public static UserReport of(User reporter, Long reportedId, ReportContentType contentType, String reason,
                                User reportedUser, String targetSnapshot) {
        return new UserReport(
                null, reporter, reportedId, contentType, reason, ReportStatus.RECEIVED, reportedUser, targetSnapshot
        );
    }

    public void updateStatus(ReportStatus status) {
        this.status = status;
    }
}
