package com.backend.orbitflow.domain.comment.service;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.comment.dto.response.CommentResponse;
import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.comment.error.CommentErrorCode;
import com.backend.orbitflow.domain.comment.repository.CommentRepository;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.dto.PostCount;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.backend.orbitflow.domain.comment.dto.ReplyCount;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

// 댓글과 대댓글은 해당 게시글을 조회할 수 있는 사용자만 작성 (차단 관계가 있으면 게시글 조회 불가, 열람 확인은 CommentFacade)
@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final BlockService blockService;

    public CommentResponse createComment(User actor, Post post, Long parentCommentId, String content) {
        Comment parent = null;
        if (parentCommentId != null) {
            parent = getActiveComment(parentCommentId);
            if (parent.isReply() || !parent.getPost().getId().equals(post.getId())) {
                throw new CommonException(CommentErrorCode.INVALID_PARENT_COMMENT);
            }
            if (!parent.isAuthor(actor) && blockService.isBlocked(parent.getUser(), actor)) {
                throw new CommonException(CommentErrorCode.COMMENT_BLOCKED);
            }
        }
        return CommentResponse.from(commentRepository.save(Comment.of(post, actor, parent, content)));
    }

    // 최상위 댓글 단위로 페이지, 각 댓글에 대댓글 포함 (viewer와 차단 관계인 작성자의 댓글 제외)
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(User viewer, Post post, int page, int size) {
        Page<Comment> comments = commentRepository.findTopLevelByPost(post, viewer, toPageable(page, size));
        if (comments.isEmpty()) {
            return comments.map(CommentResponse::from);
        }
        Map<Long, Long> replyCounts = commentRepository.countRepliesByParentIn(comments.getContent(), viewer).stream()
                .collect(Collectors.toMap(ReplyCount::parentId, ReplyCount::count));
        return comments.map(comment -> CommentResponse.of(comment, replyCounts.getOrDefault(comment.getId(), 0L)));
    }

    // 대댓글 목록의 부모가 될 최상위 댓글
    @Transactional(readOnly = true)
    public Comment getTopLevelComment(Long commentId) {
        Comment parent = getActiveComment(commentId);
        if (parent.isReply()) {
            throw new CommonException(CommentErrorCode.INVALID_PARENT_COMMENT);
        }
        return parent;
    }

    // 최상위 댓글의 대댓글 페이지 (게시글 열람 확인은 호출 측, 부모 댓글 작성자와 차단 관계면 목록에서 숨겨진 댓글이므로 없는 것으로 처리)
    @Transactional(readOnly = true)
    public Page<CommentResponse> getReplies(User viewer, Comment parent, int page, int size) {
        if (!parent.isAuthor(viewer) && blockService.isBlocked(parent.getUser(), viewer)) {
            throw new CommonException(CommentErrorCode.COMMENT_NOT_FOUND);
        }
        return commentRepository.findRepliesByParent(parent, viewer, toPageable(page, size)).map(CommentResponse::from);
    }

    public CommentResponse updateComment(User actor, Long commentId, String content) {
        Comment comment = getActiveComment(commentId);
        if (!comment.isAuthor(actor)) {
            throw new CommonException(CommentErrorCode.NOT_COMMENT_AUTHOR);
        }
        comment.updateContent(content);
        return CommentResponse.from(comment);
    }

    // 댓글 작성자 또는 게시글 작성자가 삭제, 최상위 댓글 삭제 시 대댓글은 DB가 연쇄 삭제
    public void deleteComment(User actor, Long commentId) {
        Comment comment = getActiveComment(commentId);
        if (!comment.isAuthor(actor) && !comment.getPost().isAuthor(actor)) {
            throw new CommonException(CommentErrorCode.COMMENT_DELETE_DENIED);
        }
        commentRepository.deleteById(comment.getId());
    }

    // 열람 권한·작성자 상태와 무관하게 조회 (신고 처리 등 호출 측에서 판단)
    @Transactional(readOnly = true)
    public Optional<Comment> findById(Long commentId) {
        return commentRepository.findById(commentId);
    }

    // 댓글 수 (목록과 같은 조건 : 탈퇴한 작성자, viewer와 차단 관계인 작성자 제외)
    @Transactional(readOnly = true)
    public long countByPost(Post post, User viewer) {
        return commentRepository.countByPost(post, viewer);
    }

    // 게시글별 댓글 수 (게시글 묶음 단위로 1회 조회, 댓글이 없는 게시글은 맵에 없음)
    @Transactional(readOnly = true)
    public Map<Long, Long> countByPostIn(Collection<Post> posts, User viewer) {
        return commentRepository.countByPostIn(posts, viewer).stream()
                .collect(Collectors.toMap(PostCount::postId, PostCount::count));
    }

    // 탈퇴한 작성자의 댓글은 존재하지 않는 것으로 처리
    private Comment getActiveComment(Long commentId) {
        return commentRepository.findWithAllById(commentId)
                .filter(comment -> comment.getUser().getDeletedAt() == null)
                .orElseThrow(() -> new CommonException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
