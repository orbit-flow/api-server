package com.backend.orbitflow.domain.post.service;

import com.backend.orbitflow.domain.post.dto.response.PostResponse;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostService {

    Post getViewablePost(User viewer, Long postId);
    PostResponse createPost(User actor, Long todoId, String content, List<MultipartFile> images);
    PostResponse getPost(User viewer, Long postId);
    Page<PostResponse> getTodoPosts(User viewer, Long todoId, int page, int size);
    Page<PostResponse> getUserPosts(User viewer, User author, int page, int size);
    PostResponse updatePost(User actor, Long postId, String content, List<String> keepImageUrls, List<MultipartFile> newImages);
    void deletePost(User actor, Long postId);
    List<PostResponse> toResponses(List<Post> posts, User viewer);
}
