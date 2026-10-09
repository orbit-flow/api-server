package com.backend.orbitflow.domain.report.service;

import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.comment.repository.CommentRepository;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.repository.PostRepository;
import com.backend.orbitflow.domain.report.dto.response.ReportResponse;
import com.backend.orbitflow.domain.report.entity.UserReport;
import com.backend.orbitflow.domain.report.enums.ReportContentType;
import com.backend.orbitflow.domain.report.enums.ReportStatus;
import com.backend.orbitflow.domain.report.error.ReportErrorCode;
import com.backend.orbitflow.domain.report.repository.UserReportRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.repository.UserRepository;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    private static final int SUMMARY_LENGTH = 50;

    private final UserReportRepository userReportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    // 자기 자신·자신의 콘텐츠는 신고 불가, 같은 대상에 처리 중인 신고가 있으면 중복 접수 불가
    public ReportResponse createReport(User reporter, ReportContentType contentType, Long targetId, User targetUser, String reason) {
        Long reportedId = switch (contentType) {
            case POST -> {
                Post post = postRepository.findById(requireTargetId(targetId)).orElseThrow(
                        () -> new CommonException(ReportErrorCode.REPORT_TARGET_NOT_FOUND)
                );
                validateNotSelf(reporter, post.getUser());
                yield post.getId();
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(requireTargetId(targetId)).orElseThrow(
                        () -> new CommonException(ReportErrorCode.REPORT_TARGET_NOT_FOUND)
                );
                validateNotSelf(reporter, comment.getUser());
                yield comment.getId();
            }
            case USER -> {
                if (targetUser == null) {
                    throw new CommonException(ReportErrorCode.INVALID_REPORT_TARGET);
                }
                validateNotSelf(reporter, targetUser);
                yield targetUser.getId();
            }
            // TODO: 채팅 도메인 구현 후 DM 신고 지원
            case DM -> throw new CommonException(ReportErrorCode.UNSUPPORTED_REPORT_TYPE);
        };

        if (userReportRepository.existsByReporterAndContentTypeAndReportedIdAndStatusIn(
                reporter, contentType, reportedId, ReportStatus.IN_PROGRESS)) {
            throw new CommonException(ReportErrorCode.DUPLICATE_REPORT);
        }
        UserReport report = userReportRepository.save(UserReport.of(reporter, reportedId, contentType, reason));
        return ReportResponse.of(report, resolveTarget(report));
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getMyReports(User reporter, int page, int size) {
        return userReportRepository.findAllByReporterOrderByCreatedAtDesc(reporter, toPageable(page, size))
                .map(report -> ReportResponse.of(report, resolveTarget(report)));
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> searchReports(ReportStatus status, ReportContentType contentType, int page, int size) {
        return userReportRepository.search(status, contentType, toPageable(page, size))
                .map(report -> ReportResponse.of(report, resolveTarget(report)));
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(Long reportId) {
        UserReport report = getReportEntity(reportId);
        return ReportResponse.of(report, resolveTarget(report));
    }

    public ReportResponse updateStatus(Long reportId, ReportStatus status) {
        UserReport report = getReportEntity(reportId);
        if (!report.getStatus().canTransitTo(status)) {
            throw new CommonException(ReportErrorCode.INVALID_STATUS_TRANSITION);
        }
        report.updateStatus(status);
        // TODO: 알림 도메인 구현 후 신고자에게 처리 결과 고지
        return ReportResponse.of(report, resolveTarget(report));
    }

    // 정지 처리 시 호출 : 위반이 확인된(CONFIRMED) 신고의 피신고자와 정지 대상이 같아야 함
    public void sanction(Long reportId, User suspendedUser) {
        UserReport report = getReportEntity(reportId);
        if (!report.getStatus().canTransitTo(ReportStatus.SANCTIONED)) {
            throw new CommonException(ReportErrorCode.INVALID_STATUS_TRANSITION);
        }
        Long reportedUserId = switch (report.getContentType()) {
            case POST -> postRepository.findById(report.getReportedId()).map(post -> post.getUser().getId()).orElse(null);
            case COMMENT -> commentRepository.findById(report.getReportedId()).map(comment -> comment.getUser().getId()).orElse(null);
            case USER -> report.getReportedId();
            case DM -> null;
        };
        if (!suspendedUser.getId().equals(reportedUserId)) {
            throw new CommonException(ReportErrorCode.REPORT_TARGET_MISMATCH);
        }
        report.updateStatus(ReportStatus.SANCTIONED);
    }

    private UserReport getReportEntity(Long reportId) {
        return userReportRepository.findWithReporterById(reportId).orElseThrow(
                () -> new CommonException(ReportErrorCode.REPORT_NOT_FOUND)
        );
    }

    // 신고 대상 요약 (대상이 삭제되었으면 exists = false)
    private ReportResponse.Target resolveTarget(UserReport report) {
        Long reportedId = report.getReportedId();
        return switch (report.getContentType()) {
            case POST -> postRepository.findById(reportedId)
                    .map(post -> new ReportResponse.Target(post.getId(), post.getUser().getUuid(), summarize(post.getContent()), true))
                    .orElse(ReportResponse.Target.missing(reportedId));
            case COMMENT -> commentRepository.findById(reportedId)
                    .map(comment -> new ReportResponse.Target(comment.getId(), comment.getUser().getUuid(), summarize(comment.getContent()), true))
                    .orElse(ReportResponse.Target.missing(reportedId));
            case USER -> userRepository.findById(reportedId)
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

    private String summarize(String content) {
        return content.length() <= SUMMARY_LENGTH ? content : content.substring(0, SUMMARY_LENGTH) + "...";
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), size);
    }
}
