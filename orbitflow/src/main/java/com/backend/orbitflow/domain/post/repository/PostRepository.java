package com.backend.orbitflow.domain.post.repository;

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
import com.backend.orbitflow.domain.category.repository.CategoryRepository;

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

    // 투두의 게시글 (탈퇴한 작성자, viewer와 차단 관계인 작성자 제외), onlyAuthorId가 있으면 해당 작성자의 게시글만
    @Query(value = """
            select p from Post p
            join fetch p.user u
            join fetch p.todo
            where p.todo = :todo
              and u.deletedAt is null
              and (:onlyAuthorId is null or u.id = :onlyAuthorId)
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            order by p.createdAt desc
            """,
            countQuery = """
            select count(p) from Post p
            join p.user u
            where p.todo = :todo
              and u.deletedAt is null
              and (:onlyAuthorId is null or u.id = :onlyAuthorId)
              and not exists (select b.id from Block b
                              where (b.blocker = :viewer and b.blockee = u) or (b.blocker = u and b.blockee = :viewer))
            """)
    Page<Post> findAllByTodo(@Param("todo") Todo todo, @Param("viewer") User viewer, @Param("onlyAuthorId") Long onlyAuthorId, Pageable pageable);

    // 작성자의 게시글 중 조회 사용자가 볼 수 있는 것 (게시글 id 페이지, 최신순)
    // 개인 카테고리 : PERSONAL_CATEGORY_VIEWABLE / 팀 카테고리 : TEAM_CATEGORY_VIEWABLE
    // 삭제된 팀, 소유자가 탈퇴한 카테고리 제외 (차단 관계는 호출 측에서 먼저 확인)
    String VIEWABLE_AUTHOR_POSTS = """
            from posts p
            join todos t on t.id = p.todo_id
            join categories c on c.id = t.category_id
            left join users cu on cu.id = c.user_id
            left join teams ct on ct.id = c.team_id
            where p.user_id = :authorId
              and ((c.team_id is null and cu.deleted_at is null and
            """ + CategoryRepository.PERSONAL_CATEGORY_VIEWABLE + """
                   ) or (c.team_id is not null and ct.deleted_at is null and
            """ + CategoryRepository.TEAM_CATEGORY_VIEWABLE + "))";

    @Query(nativeQuery = true,
            value = "select p.id " + VIEWABLE_AUTHOR_POSTS + " order by p.created_at desc, p.id desc",
            countQuery = "select count(*) " + VIEWABLE_AUTHOR_POSTS)
    Page<Long> findViewableIdsByAuthor(@Param("authorId") Long authorId, @Param("viewerId") Long viewerId, Pageable pageable);

    @Query("select p from Post p join fetch p.user join fetch p.todo where p.id in :ids")
    List<Post> findAllWithUserAndTodoByIdIn(@Param("ids") Collection<Long> ids);


}
