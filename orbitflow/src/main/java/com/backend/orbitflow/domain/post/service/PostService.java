package com.backend.orbitflow.domain.post.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.post.dto.response.PostResponse;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface PostService {

    // 열람 권한 확인은 호출하는 파사드가 차단·카테고리 판정 결과를 조회해 checkNotBlocked -> checkCategoryViewable 순으로 전달
    Post getActivePost(Long postId);
    Optional<Post> findById(Long postId);
    Todo requireTodo(Optional<Todo> found, boolean includeDeleted);
    void checkNotBlocked(Post post, User viewer, boolean blocked);
    void checkCategoryViewable(Post post, User viewer, boolean canView);
    void checkAuthor(Post post, User actor);
    PostResponse createPost(User actor, Todo todo, Category category, boolean teamMember, String content, List<MultipartFile> images);
    PostResponse getPost(Post post, long likeCount, long commentCount, boolean liked);
    Page<Post> findTodoPosts(User viewer, Todo todo, boolean canViewCategory, int page, int size);
    Page<Post> findUserPosts(User viewer, User author, boolean blocked, int page, int size);
    Page<PostResponse> toResponses(Page<Post> posts, Map<Long, Long> likeCounts, Map<Long, Long> commentCounts, Set<Long> likedIds);
    PostResponse updatePost(User actor, Post post, String content, List<String> keepImageUrls, List<MultipartFile> newImages,
                            long likeCount, long commentCount, boolean liked);
    void deletePost(User actor, Long postId);
}
