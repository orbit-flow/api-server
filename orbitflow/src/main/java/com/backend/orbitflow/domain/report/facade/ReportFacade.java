package com.backend.orbitflow.domain.report.facade;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.comment.service.CommentService;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.facade.PostFacade;
import com.backend.orbitflow.domain.post.service.PostService;
import com.backend.orbitflow.domain.report.dto.request.ReportRequest;
import com.backend.orbitflow.domain.report.dto.request.ReportStatusRequest;
import com.backend.orbitflow.domain.report.dto.response.ReportResponse;
import com.backend.orbitflow.domain.report.entity.UserReport;
import com.backend.orbitflow.domain.report.enums.ReportContentType;
import com.backend.orbitflow.domain.report.enums.ReportStatus;
import com.backend.orbitflow.domain.report.service.ReportService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.backend.orbitflow.domain.report.dto.response.ReportListResponse;
import com.backend.orbitflow.global.common.error.exception.CommonException;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReportFacade {

    private final ReportService reportService;
    private final UserService userService;
    private final CommentService commentService;
    private final PostService postService;
    private final PostFacade postFacade;
    private final BlockService blockService;

    // 신고자 행 락(PESSIMISTIC_WRITE)이 최신 상태를 보도록 READ_COMMITTED 필수, 락과 신고 저장은 같은 트랜잭션
    // 대상 조회(열람 가능한 게시글·댓글·차단 관계)는 락 이후에 하고, 판정 순서는 ReportService가 유지
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReportResponse createReport(AuthUser authUser, ReportRequest request) {
        User targetUser = request.contentType() == ReportContentType.USER && request.targetUserUuid() != null
                ? userService.getByUuid(request.targetUserUuid())
                : null;
        User reporter = userService.getActiveByUuid(authUser.getUuid());
        userService.lockUser(reporter.getId());

        Long targetId = request.targetId();
        Optional<Post> viewablePost = Optional.empty();
        Optional<Comment> targetComment = Optional.empty();
        boolean blocked = false;
        switch (request.contentType()) {
            case POST -> {
                if (targetId != null) {
                    viewablePost = findViewablePost(reporter, targetId);
                }
            }
            case COMMENT -> {
                if (targetId != null) {
                    targetComment = commentService.findById(targetId);
                    if (targetComment.isPresent()) {
                        Comment comment = targetComment.get();
                        viewablePost = findViewablePost(reporter, comment.getPost().getId());
                        blocked = blockService.isBlocked(comment.getUser(), reporter);
                    }
                }
            }
            case USER -> blocked = targetUser != null && blockService.isBlocked(targetUser, reporter);
            default -> {
            }
        }
        return toResponse(reportService.createReport(
                reporter, request.contentType(), targetId, targetUser, viewablePost, targetComment, blocked, request.reason()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportListResponse> getMyReports(AuthUser authUser, int page, int size) {
        return PageResponse.from(reportService.getMyReports(userService.getActiveByUuid(authUser.getUuid()), page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportListResponse> searchReports(ReportStatus status, ReportContentType contentType, int page, int size) {
        return PageResponse.from(reportService.searchReports(status, contentType, page, size));
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(Long reportId) {
        return toResponse(reportService.getReport(reportId));
    }

    @Transactional
    public ReportResponse updateStatus(Long reportId, ReportStatusRequest request) {
        return toResponse(reportService.updateStatus(reportId, request.status()));
    }

    // 게시글 단건 조회와 같은 기준(차단·카테고리 공개 범위)으로 확인, 실패 사유와 관계없이 열람 불가(empty)로 전달
    private Optional<Post> findViewablePost(User reporter, Long postId) {
        try {
            return Optional.of(postFacade.getViewablePost(reporter, postId));
        } catch (CommonException e) {
            return Optional.empty();
        }
    }

    // 신고 대상 요약 : 신고 유형에 맞는 대상만 조회 (대상이 삭제되었으면 empty)
    private ReportResponse toResponse(UserReport report) {
        Long reportedId = report.getReportedId();
        ReportContentType type = report.getContentType();
        return reportService.toResponse(report,
                type == ReportContentType.POST ? postService.findById(reportedId) : Optional.empty(),
                type == ReportContentType.COMMENT ? commentService.findById(reportedId) : Optional.empty(),
                type == ReportContentType.USER ? userService.findById(reportedId) : Optional.empty());
    }
}
