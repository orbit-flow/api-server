package com.backend.orbitflow.domain.report.service;

import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.report.dto.response.ReportResponse;
import com.backend.orbitflow.domain.report.entity.UserReport;
import com.backend.orbitflow.domain.report.enums.ReportContentType;
import com.backend.orbitflow.domain.report.enums.ReportStatus;
import com.backend.orbitflow.domain.report.error.ReportErrorCode;
import com.backend.orbitflow.domain.report.repository.UserReportRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import com.backend.orbitflow.domain.report.dto.response.ReportListResponse;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    private static final int SUMMARY_LENGTH = 50;
    private static final int SNAPSHOT_LENGTH = 500;
    private static final int DAILY_REPORT_LIMIT = 20;

    private final UserReportRepository userReportRepository;

    // 자기 자신·자신의 콘텐츠는 신고 불가, 같은 대상에 처리 중인 신고가 있으면 중복 접수 불가
    // 신고자가 볼 수 없는 대상(차단·비공개 등)은 존재 여부가 드러나지 않도록 없는 대상으로 처리, 하루 신고 횟수 제한
    // 신고자 행 락은 호출하는 파사드가 같은 트랜잭션에서 먼저 잡아 중복 확인과 저장을 직렬화 (파사드 트랜잭션은 READ_COMMITTED 필수)
    // 대상 조회(열람 가능한 게시글·댓글·차단 관계)는 다른 도메인 서비스가 필요해 파사드가 미리 조회해 전달하고, 판정 순서는 여기서 그대로 유지
    // viewablePost : POST는 대상 게시글, COMMENT는 댓글이 달린 게시글 (열람 불가면 empty), blocked : COMMENT는 댓글 작성자, USER는 대상 사용자와의 차단 관계
    public UserReport createReport(User reporter, ReportContentType contentType, Long targetId, User targetUser,
                                   Optional<Post> viewablePost, Optional<Comment> targetComment, boolean blocked, String reason) {
        if (userReportRepository.countByReporterAndCreatedAtGreaterThanEqual(reporter, LocalDate.now().atStartOfDay()) >= DAILY_REPORT_LIMIT) {
            throw new CommonException(ReportErrorCode.REPORT_LIMIT_EXCEEDED);
        }
        User reportedUser;
        String targetSnapshot;
        Long reportedId = switch (contentType) {
            case POST -> {
                requireTargetId(targetId);
                Post post = viewablePost.orElseThrow(() -> new CommonException(ReportErrorCode.REPORT_TARGET_NOT_FOUND));
                validateNotSelf(reporter, post.getUser());
                reportedUser = post.getUser();
                targetSnapshot = snapshotOf(post.getContent());
                yield post.getId();
            }
            case COMMENT -> {
                requireTargetId(targetId);
                Comment comment = targetComment.orElseThrow(
                        () -> new CommonException(ReportErrorCode.REPORT_TARGET_NOT_FOUND)
                );
                validateNotSelf(reporter, comment.getUser());
                // 댓글이 달린 게시글을 볼 수 없거나 댓글 작성자와 차단 관계이면 없는 대상으로 처리
                if (viewablePost.isEmpty()) {
                    throw new CommonException(ReportErrorCode.REPORT_TARGET_NOT_FOUND);
                }
                if (comment.getUser().getDeletedAt() != null || blocked) {
                    throw new CommonException(ReportErrorCode.REPORT_TARGET_NOT_FOUND);
                }
                reportedUser = comment.getUser();
                targetSnapshot = snapshotOf(comment.getContent());
                yield comment.getId();
            }
            case USER -> {
                if (targetUser == null) {
                    throw new CommonException(ReportErrorCode.INVALID_REPORT_TARGET);
                }
                validateNotSelf(reporter, targetUser);
                if (blocked) {
                    throw new CommonException(ReportErrorCode.REPORT_TARGET_NOT_FOUND);
                }
                reportedUser = targetUser;
                targetSnapshot = snapshotOf(targetUser.getName());
                yield targetUser.getId();
            }
            // TODO: 채팅 도메인 구현 후 DM 신고 지원
            case DM -> throw new CommonException(ReportErrorCode.UNSUPPORTED_REPORT_TYPE);
        };

        if (userReportRepository.existsByReporterAndContentTypeAndReportedIdAndStatusIn(
                reporter, contentType, reportedId, ReportStatus.IN_PROGRESS)) {
            throw new CommonException(ReportErrorCode.DUPLICATE_REPORT);
        }
        return userReportRepository.save(UserReport.of(reporter, reportedId, contentType, reason, reportedUser, targetSnapshot));
    }

    @Transactional(readOnly = true)
    public Page<ReportListResponse> getMyReports(User reporter, int page, int size) {
        return userReportRepository.findListByReporter(reporter, toPageable(page, size));
    }

    @Transactional(readOnly = true)
    public Page<ReportListResponse> searchReports(ReportStatus status, ReportContentType contentType, int page, int size) {
        return userReportRepository.search(status, contentType, toPageable(page, size));
    }

    @Transactional(readOnly = true)
    public UserReport getReport(Long reportId) {
        return getReportEntity(reportId);
    }

    public UserReport updateStatus(Long reportId, ReportStatus status) {
        UserReport report = getReportEntity(reportId);
        if (!report.getStatus().canTransitTo(status)) {
            throw new CommonException(ReportErrorCode.INVALID_STATUS_TRANSITION);
        }
        report.updateStatus(status);
        // TODO: 알림 도메인 구현 후 신고자에게 처리 결과 고지
        return report;
    }

    // 정지 처리 시 호출 : 위반이 확인된(CONFIRMED) 신고의 피신고자와 정지 대상이 같아야 함
    public void sanction(Long reportId, User suspendedUser) {
        UserReport report = getReportEntity(reportId);
        if (!report.getStatus().canTransitTo(ReportStatus.SANCTIONED)) {
            throw new CommonException(ReportErrorCode.INVALID_STATUS_TRANSITION);
        }
        // 신고 시점에 보관한 피신고자 기준이라 신고된 게시글·댓글이 삭제되어도 제재 가능
        if (!suspendedUser.getId().equals(report.getReportedUser().getId())) {
            throw new CommonException(ReportErrorCode.REPORT_TARGET_MISMATCH);
        }
        report.updateStatus(ReportStatus.SANCTIONED);
    }

    private UserReport getReportEntity(Long reportId) {
        return userReportRepository.findWithReporterById(reportId).orElseThrow(
                () -> new CommonException(ReportErrorCode.REPORT_NOT_FOUND)
        );
    }

    // 신고 대상 요약 (대상이 삭제되었으면 exists = false), 대상 조회는 호출 측에서 신고 유형에 맞는 것만 조회해 전달
    @Transactional(readOnly = true)
    public ReportResponse toResponse(UserReport report, Optional<Post> targetPost, Optional<Comment> targetComment, Optional<User> targetUser) {
        return ReportResponse.of(report, resolveTarget(report, targetPost, targetComment, targetUser));
    }

    private ReportResponse.Target resolveTarget(UserReport report, Optional<Post> targetPost, Optional<Comment> targetComment, Optional<User> targetUser) {
        Long reportedId = report.getReportedId();
        return switch (report.getContentType()) {
            case POST -> targetPost
                    .map(post -> new ReportResponse.Target(post.getId(), post.getUser().getUuid(), summarize(post.getContent()), true))
                    .orElse(ReportResponse.Target.missing(reportedId));
            case COMMENT -> targetComment
                    .map(comment -> new ReportResponse.Target(comment.getId(), comment.getUser().getUuid(), summarize(comment.getContent()), true))
                    .orElse(ReportResponse.Target.missing(reportedId));
            case USER -> targetUser
                    .map(user -> new ReportResponse.Target(null, user.getUuid(), user.getName(), user.getDeletedAt() == null))
                    .orElse(ReportResponse.Target.missing(null));
            case DM -> ReportResponse.Target.missing(reportedId);
        };
    }

    private Long requireTargetId(Long targetId) {
        if (targetId == null) {
            throw new CommonException(ReportErrorCode.INVALID_REPORT_TARGET);
        }
        return targetId;
    }

    private void validateNotSelf(User reporter, User target) {
        if (reporter.getId().equals(target.getId())) {
            throw new CommonException(ReportErrorCode.SELF_REPORT);
        }
    }

    // 증거 보관용 : 요약과 달리 말줄임 없이 컬럼 길이까지 그대로 저장
    private String snapshotOf(String content) {
        return content.length() <= SNAPSHOT_LENGTH ? content : content.substring(0, SNAPSHOT_LENGTH);
    }

    private String summarize(String content) {
        return content.length() <= SUMMARY_LENGTH ? content : content.substring(0, SUMMARY_LENGTH) + "...";
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
