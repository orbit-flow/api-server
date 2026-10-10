package com.backend.orbitflow.domain.comment.entity;

import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// parentComment가 있으면 대댓글 (한 단계까지만 허용)
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 최상위 댓글 목록 (post_id = ? and parent_comment_id is null order by created_at, id) 조건·정렬 순서와 일치 (id는 InnoDB 보조 인덱스에 포함)
@Table(name = "comments",
        indexes = @Index(columnList = "post_id, parent_comment_id, created_at"))
public class Comment extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment parentComment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public static Comment of(Post post, User user, Comment parentComment, String content) {
        return new Comment(
                null, post, user, parentComment, content
        );
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public boolean isReply() {
        return this.parentComment != null;
    }

    public boolean isAuthor(User user) {
        return this.user.getId().equals(user.getId());
    }
}
