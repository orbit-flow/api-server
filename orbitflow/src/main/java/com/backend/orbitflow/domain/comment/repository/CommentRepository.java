package com.backend.orbitflow.domain.comment.repository;

import com.backend.orbitflow.domain.comment.entity.Comment;
import com.backend.orbitflow.domain.post.dto.PostCount;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    long countByPost(Post post);

    @Query("""
            select new com.backend.orbitflow.domain.post.dto.PostCount(c.post.id, count(c))
            from Comment c
            where c.post in :posts
            group by c.post.id
            """)
    List<PostCount> countByPostIn(@Param("posts") Collection<Post> posts);

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

    // 최상위 댓글들의 대댓글 (탈퇴한 작성자, viewer와 차단 관계인 작성자 제외)
    @Query("""
            select c from Comment c
            join fetch c.user u
            where c.parentComment in :parents
              and u.deletedAt is null
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            order by c.createdAt asc, c.id asc
            """)
    List<Comment> findRepliesByParentIn(@Param("parents") Collection<Comment> parents, @Param("viewer") User viewer);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Comment c where c.parentComment = :parent")
    void deleteAllByParent(@Param("parent") Comment parent);

    // 자기 참조 FK 때문에 대댓글을 먼저 삭제
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Comment c where c.post = :post and c.parentComment is not null")
    void deleteAllRepliesByPost(@Param("post") Post post);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Comment c where c.post = :post")
    void deleteAllByPost(@Param("post") Post post);
}
