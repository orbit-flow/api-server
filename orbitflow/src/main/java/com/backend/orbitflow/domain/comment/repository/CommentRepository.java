package com.backend.orbitflow.domain.comment.repository;

import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.post.dto.PostCount;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.backend.orbitflow.domain.comment.dto.ReplyCount;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 게시글의 댓글 수 (목록과 같은 조건 : 탈퇴한 작성자, viewer와 차단 관계인 작성자 제외)
    @Query("""
            select count(c) from Comment c
            join c.user u
            where c.post = :post
              and u.deletedAt is null
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            """)
    long countByPost(@Param("post") Post post, @Param("viewer") User viewer);

    @Query("""
            select new com.backend.orbitflow.domain.post.dto.PostCount(c.post.id, count(c))
            from Comment c
            join c.user u
            where c.post in :posts
              and u.deletedAt is null
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            group by c.post.id
            """)
    List<PostCount> countByPostIn(@Param("posts") Collection<Post> posts, @Param("viewer") User viewer);

    @Query("""
            select c from Comment c
            join fetch c.user
            join fetch c.post
            left join fetch c.parentComment
            where c.id = :id
            """)
    Optional<Comment> findWithAllById(@Param("id") Long id);

    // 게시글의 최상위 댓글 (탈퇴한 작성자, viewer와 차단 관계인 작성자 제외)
    @Query(value = """
            select c from Comment c
            join fetch c.user u
            where c.post = :post
              and c.parentComment is null
              and u.deletedAt is null
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            order by c.createdAt asc, c.id asc
            """,
            countQuery = """
            select count(c) from Comment c
            join c.user u
            where c.post = :post
              and c.parentComment is null
              and u.deletedAt is null
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            """)
    Page<Comment> findTopLevelByPost(@Param("post") Post post, @Param("viewer") User viewer, Pageable pageable);

    // 최상위 댓글들의 대댓글 수 (목록과 같은 조건 : 탈퇴한 작성자, viewer와 차단 관계인 작성자 제외)
    @Query("""
            select new com.backend.orbitflow.domain.comment.dto.ReplyCount(c.parentComment.id, count(c))
            from Comment c
            join c.user u
            where c.parentComment in :parents
              and u.deletedAt is null
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            group by c.parentComment.id
            """)
    List<ReplyCount> countRepliesByParentIn(@Param("parents") Collection<Comment> parents, @Param("viewer") User viewer);

    // 대댓글 페이지 (작성 순)
    @Query(value = """
            select c from Comment c
            join fetch c.user u
            where c.parentComment = :parent
              and u.deletedAt is null
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            order by c.createdAt asc, c.id asc
            """,
            countQuery = """
            select count(c) from Comment c
            join c.user u
            where c.parentComment = :parent
              and u.deletedAt is null
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            """)
    Page<Comment> findRepliesByParent(@Param("parent") Comment parent, @Param("viewer") User viewer, Pageable pageable);
}
