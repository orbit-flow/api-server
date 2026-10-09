package com.backend.orbitflow.domain.post.repository;

import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findAllByPostOrderBySortOrderAsc(Post post);

    List<PostImage> findAllByPostInOrderBySortOrderAsc(Collection<Post> posts);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PostImage pi where pi.post = :post")
    void deleteAllByPost(@Param("post") Post post);
}
