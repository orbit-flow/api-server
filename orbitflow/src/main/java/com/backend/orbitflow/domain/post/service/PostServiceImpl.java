package com.backend.orbitflow.domain.post.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.post.dto.response.PostResponse;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.entity.PostImage;
import com.backend.orbitflow.domain.post.error.PostErrorCode;
import com.backend.orbitflow.domain.post.repository.PostImageRepository;
import com.backend.orbitflow.domain.post.repository.PostRepository;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.error.TodoErrorCode;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.util.JdbcBulkInserter;
import com.backend.orbitflow.global.util.S3TransactionalFileManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PostServiceImpl implements PostService {

    private static final String IMAGE_DIR = "posts";

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final S3TransactionalFileManager s3FileManager;
    private final JdbcBulkInserter jdbcBulkInserter;

    // 탈퇴한 작성자의 게시글은 존재하지 않는 것으로 처리
    // 열람 권한 확인(차단·카테고리 공개 범위)은 다른 도메인 서비스가 필요해 PostFacade에서 조율하고, 판정 결과만 checkNotBlocked·checkCategoryViewable로 검증
    @Transactional(readOnly = true)
    public Post getActivePost(Long postId) {
        return postRepository.findWithAllById(postId)
                .filter(post -> post.getUser().getDeletedAt() == null)
                .orElseThrow(() -> new CommonException(PostErrorCode.POST_NOT_FOUND));
    }

    // 조회된 투두가 없으면 오류, 게시글 작성은 논리적 삭제된 투두 불가 (투두 조회는 호출 측에서)
    public Todo requireTodo(Optional<Todo> found, boolean includeDeleted) {
        return found
                .filter(todo -> includeDeleted || !todo.isDeleted())
                .orElseThrow(() -> new CommonException(TodoErrorCode.TODO_NOT_FOUND));
    }

    // 차단 관계가 우선, 작성자 본인은 제외
    public void checkNotBlocked(Post post, User viewer, boolean blocked) {
        if (!post.isAuthor(viewer) && blocked) {
            throw new CommonException(PostErrorCode.POST_ACCESS_DENIED);
        }
    }

    // 연결된 투두의 카테고리 공개 범위, 작성자 본인은 제외
    public void checkCategoryViewable(Post post, User viewer, boolean canView) {
        if (!post.isAuthor(viewer) && !canView) {
            throw new CommonException(PostErrorCode.POST_ACCESS_DENIED);
        }
    }

    public PostResponse createPost(User actor, Todo todo, Category category, boolean teamMember, String content, List<MultipartFile> images) {
        checkWrite(todo, category, actor, teamMember);
        List<MultipartFile> files = nonEmpty(images);
        if (files.size() > Post.MAX_IMAGE_COUNT) {
            throw new CommonException(PostErrorCode.IMAGE_COUNT_EXCEED);
        }

        Post post = postRepository.save(Post.of(todo, actor, content));
        List<PostImage> postImages = saveImages(post, s3FileManager.upload(IMAGE_DIR, files), 0);
        // 팔로워 타임라인에는 조회 시점에 노출 (TimelineService)
        return PostResponse.of(post, postImages, 0, 0, false);
    }

    // 열람 권한을 확인한 게시글의 응답 조립, 좋아요·댓글 수와 좋아요 여부는 호출 측에서 조회해 전달
    @Transactional(readOnly = true)
    public PostResponse getPost(Post post, long likeCount, long commentCount, boolean liked) {
        return PostResponse.of(post, postImageRepository.findAllByPostOrderBySortOrderAsc(post), likeCount, commentCount, liked);
    }

    // 투두가 삭제되어도 게시글은 조회 가능
    @Transactional(readOnly = true)
    public Page<Post> findTodoPosts(User viewer, Todo todo, boolean canViewCategory, int page, int size) {
        // 카테고리를 볼 수 없어도 단건 조회와 같이 본인이 작성한 게시글은 조회 가능
        Long onlyAuthorId = canViewCategory ? null : viewer.getId();
        return postRepository.findAllByTodo(todo, viewer, onlyAuthorId, toPageable(page, size));
    }

    // 작성자의 게시글 중 viewer가 열람 가능한 카테고리의 게시글만 (viewer·작성자 간 차단 관계는 호출 측이 조회해 전달, 본인 글이면 무시)
    @Transactional(readOnly = true)
    public Page<Post> findUserPosts(User viewer, User author, boolean blocked, int page, int size) {
        Pageable pageable = toPageable(page, size);
        if (!viewer.getId().equals(author.getId()) && blocked) {
            return Page.empty(pageable);
        }
        // 열람 판정은 조회 쿼리에서 (목록 id 1 + 개수 1 + 본문 1 + 사진·좋아요·댓글 묶음 조회)
        Page<Long> ids = postRepository.findViewableIdsByAuthor(author.getId(), viewer.getId(), pageable);
        Map<Long, Post> posts = postRepository.findAllWithUserAndTodoByIdIn(ids.getContent()).stream()
                .collect(Collectors.toMap(Post::getId, post -> post));
        return ids.map(posts::get);
    }

    // 유지할 사진을 요청 순서대로 두고 새 사진을 뒤에 추가, 빠진 사진은 커밋 후 S3에서 삭제
    public PostResponse updatePost(User actor, Post post, String content, List<String> keepImageUrls, List<MultipartFile> newImages,
                                   long likeCount, long commentCount, boolean liked) {
        List<PostImage> current = postImageRepository.findAllByPostOrderBySortOrderAsc(post);
        Map<String, PostImage> currentByUrl = new LinkedHashMap<>();
        current.forEach(image -> currentByUrl.put(image.getImageUrl(), image));
        if (new HashSet<>(keepImageUrls).size() != keepImageUrls.size()
                || !currentByUrl.keySet().containsAll(keepImageUrls)) {
            throw new CommonException(PostErrorCode.INVALID_KEEP_IMAGE);
        }
        List<MultipartFile> files = nonEmpty(newImages);
        if (keepImageUrls.size() + files.size() > Post.MAX_IMAGE_COUNT) {
            throw new CommonException(PostErrorCode.IMAGE_COUNT_EXCEED);
        }

        List<PostImage> removed = current.stream()
                .filter(image -> !keepImageUrls.contains(image.getImageUrl()))
                .toList();
        if (!removed.isEmpty()) {
            postImageRepository.deleteAllInBatch(removed);
        }
        s3FileManager.deleteAfterCommit(removed.stream().map(PostImage::getImageUrl).toList());

        List<PostImage> result = new ArrayList<>();
        for (int i = 0; i < keepImageUrls.size(); i++) {
            PostImage kept = currentByUrl.get(keepImageUrls.get(i));
            kept.updateSortOrder(i);
            result.add(kept);
        }
        result.addAll(saveImages(post, s3FileManager.upload(IMAGE_DIR, files), keepImageUrls.size()));
        post.updateContent(content);
        return PostResponse.of(post, result, likeCount, commentCount, liked);
    }

    // 게시글 삭제 시 사진·댓글·좋아요는 DB가 연쇄 삭제, S3 사진 파일은 커밋 후 삭제
    public void deletePost(User actor, Long postId) {
        Post post = getActivePost(postId);
        checkAuthor(post, actor);
        List<String> imageUrls = postImageRepository.findAllByPostOrderBySortOrderAsc(post).stream()
                .map(PostImage::getImageUrl)
                .toList();
        postRepository.deleteById(post.getId());
        s3FileManager.deleteAfterCommit(imageUrls);
    }

    // 개인 투두는 카테고리 소유자, 팀 투두는 현재 담당자(팀 구성원)만 작성
    // 팀 구성원 여부는 호출 측에서 조회해 전달
    private void checkWrite(Todo todo, Category category, User actor, boolean teamMember) {
        boolean allowed = category.isTeamCategory()
                ? todo.getAssignee().getId().equals(actor.getId()) && teamMember
                : category.isOwnedBy(actor);
        if (!allowed) {
            throw new CommonException(PostErrorCode.POST_WRITE_DENIED);
        }
    }

    // 수정 전 호출 측이 먼저 확인 (확인 전 좋아요·댓글 수 조회를 피하기 위함)
    public void checkAuthor(Post post, User actor) {
        if (!post.isAuthor(actor)) {
            throw new CommonException(PostErrorCode.NOT_POST_AUTHOR);
        }
    }

    // 열람 권한·작성자 상태와 무관하게 조회 (관리자 신고 처리 등 호출 측에서 판단)
    @Transactional(readOnly = true)
    public Optional<Post> findById(Long postId) {
        return postRepository.findById(postId);
    }

    // 목록 응답 조립 : 좋아요·댓글 수(탈퇴한 사용자, 요청자와 차단 관계인 사용자 제외)와 요청자의 좋아요 여부는 호출 측에서 게시글 묶음 단위로 조회해 전달 (N+1 방지)
    @Transactional(readOnly = true)
    public Page<PostResponse> toResponses(Page<Post> posts, Map<Long, Long> likeCounts, Map<Long, Long> commentCounts, Set<Long> likedIds) {
        Map<Long, PostResponse> responses = toResponseMap(posts.getContent(), likeCounts, commentCounts, likedIds);
        return posts.map(post -> responses.get(post.getId()));
    }

    // 사진은 게시글 묶음 단위로 조회
    private Map<Long, PostResponse> toResponseMap(List<Post> posts, Map<Long, Long> likeCounts, Map<Long, Long> commentCounts, Set<Long> likedIds) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PostImage>> imagesByPost = postImageRepository.findAllByPostInOrderBySortOrderAsc(posts).stream()
                .collect(Collectors.groupingBy(image -> image.getPost().getId()));
        return posts.stream()
                .collect(Collectors.toMap(Post::getId, post -> PostResponse.of(
                        post,
                        imagesByPost.getOrDefault(post.getId(), List.of()),
                        likeCounts.getOrDefault(post.getId(), 0L),
                        commentCounts.getOrDefault(post.getId(), 0L),
                        likedIds.contains(post.getId())
                )));
    }

    // 사진 수와 무관하게 INSERT 1회 (JDBC 배치), 응답에는 사진 URL만 쓰이고 이후 수정하지 않으므로 반환 엔티티는 영속 상태가 아님
    private List<PostImage> saveImages(Post post, List<String> urls, int startOrder) {
        List<PostImage> images = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            images.add(PostImage.of(post, urls.get(i), startOrder + i));
        }
        jdbcBulkInserter.insert("post_images", List.of("post_id", "image_url", "sort_order"),
                images.stream()
                        .map(image -> new Object[]{post.getId(), image.getImageUrl(), image.getSortOrder()})
                        .toList());
        return images;
    }

    private List<MultipartFile> nonEmpty(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
