package com.backend.orbitflow.domain.report.facade;

import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.comment.service.CommentService;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.service.PostService;
import com.backend.orbitflow.domain.report.dto.request.ReportRequest;
import com.backend.orbitflow.domain.report.dto.request.ReportStatusRequest;
import com.backend.orbitflow.domain.report.dto.response.ReportResponse;
import com.backend.orbitflow.domain.report.entity.UserReport;
import com.backend.orbitflow.domain.report.enums.ReportContentType;
import com.backend.orbitflow.domain.report.enums.ReportStatus;
import com.backend.orbitflow.domain.report.service.ReportService;
import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.service.CategoryAuthorityService;
import com.backend.orbitflow.domain.category.service.CategoryService;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
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
    private final BlockService blockService;
    private final CategoryService categoryService;
    private final CategoryAuthorityService categoryAuthorityService;
    private final TeamAuthorityService teamAuthorityService;
    private final FollowService followService;

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
        boolean commentAuthorBlocked = false;
        boolean targetUserBlocked = false;
        // 신고 유형마다 대상 id의 종류가 달라 해당 유형의 대상만 조회 (다른 유형의 id로 조회하면 실패가 트랜잭션을 rollback-only로 만듦)
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
                        commentAuthorBlocked = blockService.isBlocked(comment.getUser(), reporter);
                    }
                }
            }
            case USER -> targetUserBlocked = targetUser != null && blockService.isBlocked(targetUser, reporter);
            default -> {
            }
        }
        return toResponse(reportService.createReport(
                reporter, request.contentType(), targetId, targetUser, viewablePost, targetComment, commentAuthorBlocked, targetUserBlocked, request.reason()));
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
            return Optional.of(getViewablePost(reporter, postId));
        } catch (CommonException e) {
            return Optional.empty();
        }
    }

    // 게시글을 조회할 수 있는 사용자만 가능 : 차단 관계 -> 연결된 투두의 카테고리 공개 범위 순으로 조회하고 판정은 PostService
    // PostFacade·CommentFacade·PostLikeFacade·ReportFacade에 같은 헬퍼가 있음 (Facade끼리 참조하지 않음)
    private Post getViewablePost(User viewer, Long postId) {
        Post post = postService.getActivePost(postId);
        postService.checkNotBlocked(post, viewer, blockService.isBlocked(post.getUser(), viewer));
        Category category = categoryService.getActiveCategory(post.getTodo().getCategory().getId());
        postService.checkCategoryViewable(post, viewer, canView(category, viewer));
        return post;
    }

    // 카테고리 열람 가능 여부 : 소유 도메인(팀·차단·팔로우)의 정보를 조회해 정책에 전달
    private boolean canView(Category category, User viewer) {
        if (category.isTeamCategory()) {
            Team team = category.getTeam();
            TeamMember member = teamAuthorityService.findMember(team, viewer).orElse(null);
            Integer mask = member == null ? null : teamAuthorityService.getPermissionMask(team, member);
            return categoryAuthorityService.canViewTeam(category, member, mask);
        }
        User owner = category.getUser();
        // 본인 카테고리는 관계를 조회할 필요가 없고, 차단 관계면 팔로우 상태는 판정에 쓰이지 않음
        boolean self = owner.getId().equals(viewer.getId());
        boolean blocked = !self && blockService.isBlocked(owner, viewer);
        FollowState followState = self || blocked ? null : followService.getFollowState(viewer, owner);
        return categoryAuthorityService.canViewPersonal(category, viewer, blocked, followState);
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
