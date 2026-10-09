package com.backend.orbitflow.domain.post.repository;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
            select p from Post p
            join fetch p.user
            join fetch p.todo t
            join fetch t.category c
            left join fetch c.user
            left join fetch c.team
            where p.id = :id
            """)
    Optional<Post> findWithAllById(@Param("id") Long id);

    // 투두의 게시글 (탈퇴한 작성자, viewer와 차단 관계인 작성자 제외)
    @Query(value = """
            select p from Post p
            join fetch p.user u
            join fetch p.todo
            where p.todo = :todo
              and u.deletedAt is null
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            order by p.createdAt desc
            """,
            countQuery = """
            select count(p) from Post p
            join p.user u
            where p.todo = :todo
              and u.deletedAt is null
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            """)
    Page<Post> findAllByTodo(@Param("todo") Todo todo, @Param("viewer") User viewer, Pageable pageable);

    // 작성자의 게시글이 속한 카테고리 목록 (열람 가능 여부 판정용)
    @Query("""
            select distinct c from Post p
            join p.todo t
            join t.category c
            left join fetch c.user
            left join fetch c.team
            where p.user = :author
            """)
    List<Category> findCategoriesByAuthor(@Param("author") User author);

    @Query(value = """
            select p from Post p
            join fetch p.user
            join fetch p.todo t
            where p.user = :author
              and t.category.id in :categoryIds
            order by p.createdAt desc
            """,
            countQuery = """
            select count(p) from Post p
            join p.todo t
            where p.user = :author
              and t.category.id in :categoryIds
            """)
    Page<Post> findAllByAuthorAndCategoryIdIn(
            @Param("author") User author,
            @Param("categoryIds") Collection<Long> categoryIds,
            Pageable pageable
    );
}
