package com.backend.orbitflow.domain.like.repository;

import com.backend.orbitflow.domain.like.entity.PostLike;
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
import java.util.Set;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostAndUser(Post post, User user);

    boolean existsByPostAndUser(Post post, User user);

    long countByPost(Post post);

    @Query("""
            select new com.backend.orbitflow.domain.post.dto.PostCount(l.post.id, count(l))
            from PostLike l
            where l.post in :posts
            group by l.post.id
            """)
    List<PostCount> countByPostIn(@Param("posts") Collection<Post> posts);

    @Query("select l.post.id from PostLike l where l.post in :posts and l.user = :user")
    Set<Long> findLikedPostIds(@Param("posts") Collection<Post> posts, @Param("user") User user);

    // 좋아요 누른 사용자 (탈퇴한 사용자, viewer와 차단 관계인 사용자 제외)
    @Query(value = """
            select l from PostLike l
            join fetch l.user u
            where l.post = :post
              and u.deletedAt is null
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            order by l.createdAt desc
            """,
            countQuery = """
            select count(l) from PostLike l
            join l.user u
            where l.post = :post
              and u.deletedAt is null
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            """)
    Page<PostLike> findAllByPost(@Param("post") Post post, @Param("viewer") User viewer, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PostLike l where l.post = :post")
    void deleteAllByPost(@Param("post") Post post);
}
