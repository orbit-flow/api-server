package com.backend.orbitflow.domain.report.service;

import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.report.dto.response.ReportResponse;
import com.backend.orbitflow.domain.report.entity.UserReport;
import com.backend.orbitflow.domain.report.enums.ReportContentType;
import com.backend.orbitflow.domain.report.enums.ReportStatus;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.Optional;
import com.backend.orbitflow.domain.report.dto.response.ReportListResponse;

public interface ReportService {

    // 대상 조회는 다른 도메인 서비스가 필요해 ReportFacade가 조회해 전달 (viewablePost : 열람 가능한 게시글, blocked : 대상 작성자·사용자와의 차단 관계)
    UserReport createReport(User reporter, ReportContentType contentType, Long targetId, User targetUser,
                            Optional<Post> viewablePost, Optional<Comment> targetComment, boolean blocked, String reason);
    Page<ReportListResponse> getMyReports(User reporter, int page, int size);

    // 관리자
    Page<ReportListResponse> searchReports(ReportStatus status, ReportContentType contentType, int page, int size);
    UserReport getReport(Long reportId);
    UserReport updateStatus(Long reportId, ReportStatus status);
    ReportResponse toResponse(UserReport report, Optional<Post> post, Optional<Comment> comment, Optional<User> user);
    void sanction(Long reportId, User suspendedUser);
}
