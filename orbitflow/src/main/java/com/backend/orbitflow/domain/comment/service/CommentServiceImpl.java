package com.backend.orbitflow.domain.comment.service;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.comment.dto.response.CommentResponse;
import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.comment.error.CommentErrorCode;
import com.backend.orbitflow.domain.comment.repository.CommentRepository;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.service.PostService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.domain.notification.event.CommentCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 댓글과 대댓글은 해당 게시글을 조회할 수 있는 사용자만 작성 (차단 관계가 있으면 게시글 조회 불가)
@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final BlockService blockService;
    private final ApplicationEventPublisher eventPublisher;

    public CommentResponse createComment(User actor, Long postId, Long parentCommentId, String content) {
        Post post = postService.getViewablePost(actor, postId);
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
        Comment comment = commentRepository.save(Comment.of(post, actor, parent, content));
        eventPublisher.publishEvent(new CommentCreatedEvent(comment.getId()));
        return CommentResponse.from(comment);
    }

    // 최상위 댓글 단위로 페이지, 각 댓글에 대댓글 포함 (viewer와 차단 관계인 작성자의 댓글 제외)
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(User viewer, Long postId, int page, int size) {
        Post post = postService.getViewablePost(viewer, postId);
        Page<Comment> comments = commentRepository.findTopLevelByPost(post, viewer, PageRequest.of(Math.max(page - 1, 0), size));
        if (comments.isEmpty()) {
            return comments.map(CommentResponse::from);
        }
        Map<Long, List<CommentResponse>> repliesByParent = commentRepository.findRepliesByParentIn(comments.getContent(), viewer).stream()
                .collect(Collectors.groupingBy(
                        reply -> reply.getParentComment().getId(),
                        Collectors.mapping(CommentResponse::from, Collectors.toList())
                ));
        return comments.map(comment -> CommentResponse.of(comment, repliesByParent.getOrDefault(comment.getId(), List.of())));
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

    // 탈퇴한 작성자의 댓글은 존재하지 않는 것으로 처리
    private Comment getActiveComment(Long commentId) {
        return commentRepository.findWithAllById(commentId)
                .filter(comment -> comment.getUser().getDeletedAt() == null)
                .orElseThrow(() -> new CommonException(CommentErrorCode.COMMENT_NOT_FOUND));
    }
}
