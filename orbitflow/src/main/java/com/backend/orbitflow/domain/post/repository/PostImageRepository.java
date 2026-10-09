package com.backend.orbitflow.domain.post.repository;

import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findAllByPostOrderBySortOrderAsc(Post post);

    List<PostImage> findAllByPostInOrderBySortOrderAsc(Collection<Post> posts);
}
