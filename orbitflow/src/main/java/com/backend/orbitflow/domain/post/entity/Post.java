package com.backend.orbitflow.domain.post.entity;

import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 투두 기반 게시글, 공개 대상은 연결된 투두가 속한 카테고리의 공개 범위를 상속 (게시글별 변경 불가)
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "posts")
public class Post extends BaseEntity {

    public static final int MAX_IMAGE_COUNT = 3;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 투두가 삭제되어도 게시글은 유지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public static Post of(Todo todo, User user, String content) {
        return new Post(
                null, todo, user, content
        );
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public boolean isAuthor(User user) {
        return this.user.getId().equals(user.getId());
    }
}
