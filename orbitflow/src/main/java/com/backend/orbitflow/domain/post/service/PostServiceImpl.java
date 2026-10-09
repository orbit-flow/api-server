package com.backend.orbitflow.domain.post.service;

import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.comment.repository.CommentRepository;
import com.backend.orbitflow.domain.like.repository.PostLikeRepository;
import com.backend.orbitflow.domain.post.dto.PostCount;
import com.backend.orbitflow.domain.category.service.CategoryAuthorityService;
import com.backend.orbitflow.domain.category.service.CategoryService;
import com.backend.orbitflow.domain.post.dto.response.PostResponse;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.entity.PostImage;
import com.backend.orbitflow.domain.post.error.PostErrorCode;
import com.backend.orbitflow.domain.post.repository.PostImageRepository;
import com.backend.orbitflow.domain.post.repository.PostRepository;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.error.TodoErrorCode;
import com.backend.orbitflow.domain.todo.repository.TodoRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.util.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostServiceImpl implements PostService {

    private static final String IMAGE_DIR = "posts";

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final TodoRepository todoRepository;
    private final CategoryService categoryService;
    private final CategoryAuthorityService categoryAuthorityService;
    private final TeamAuthorityService teamAuthorityService;
    private final BlockService blockService;
    private final S3Service s3Service;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;

    // 게시글을 조회할 수 있는 사용자만 댓글·대댓글·좋아요 가능
    @Transactional(readOnly = true)
    public Post getViewablePost(User viewer, Long postId) {
        Post post = getActivePost(postId);
        checkView(post, viewer);
        return post;
    }

    public PostResponse createPost(User actor, Long todoId, String content, List<MultipartFile> images) {
        Todo todo = todoRepository.findWithAllById(todoId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new CommonException(TodoErrorCode.TODO_NOT_FOUND));
        checkWrite(todo, actor);
        List<MultipartFile> files = nonEmpty(images);
        if (files.size() > Post.MAX_IMAGE_COUNT) {
            throw new CommonException(PostErrorCode.IMAGE_COUNT_EXCEED);
        }

        Post post = postRepository.save(Post.of(todo, actor, content));
        List<PostImage> postImages = saveImages(post, upload(files), 0);
        // TODO: 알림 도메인 구현 후 팔로워에게 NEWPOST 알림, 타임라인 노출
        return PostResponse.of(post, postImages, 0, 0, false);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(User viewer, Long postId) {
        Post post = getActivePost(postId);
        checkView(post, viewer);
        return toResponse(post, postImageRepository.findAllByPostOrderBySortOrderAsc(post), viewer);
    }

    // 투두가 삭제되어도 게시글은 조회 가능
    @Transactional(readOnly = true)
    public Page<PostResponse> getTodoPosts(User viewer, Long todoId, int page, int size) {
        Todo todo = todoRepository.findWithAllById(todoId).orElseThrow(
                () -> new CommonException(TodoErrorCode.TODO_NOT_FOUND)
        );
        Category category = categoryService.getActiveCategory(todo.getCategory().getId());
        if (!categoryAuthorityService.canView(category, viewer)) {
            throw new CommonException(PostErrorCode.POST_ACCESS_DENIED);
        }
        return toResponses(postRepository.findAllByTodo(todo, viewer, toPageable(page, size)), viewer);
    }

    // 작성자의 게시글 중 viewer가 열람 가능한 카테고리의 게시글만
    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(User viewer, User author, int page, int size) {
        Pageable pageable = toPageable(page, size);
        if (!viewer.getId().equals(author.getId()) && blockService.isBlocked(author, viewer)) {
            return Page.empty(pageable);
        }
        Set<Long> viewableIds = viewableCategoryIds(viewer, postRepository.findCategoriesByAuthor(author));
        if (viewableIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return toResponses(postRepository.findAllByAuthorAndCategoryIdIn(author, viewableIds, pageable), viewer);
    }

    // 유지할 사진을 요청 순서대로 두고 새 사진을 뒤에 추가, 빠진 사진은 커밋 후 S3에서 삭제
    public PostResponse updatePost(User actor, Long postId, String content, List<String> keepImageUrls, List<MultipartFile> newImages) {
        Post post = getActivePost(postId);
        checkAuthor(post, actor);

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
        postImageRepository.deleteAll(removed);
        deleteFilesAfterCommit(removed.stream().map(PostImage::getImageUrl).toList());

        List<PostImage> result = new ArrayList<>();
        for (int i = 0; i < keepImageUrls.size(); i++) {
            PostImage kept = currentByUrl.get(keepImageUrls.get(i));
            kept.updateSortOrder(i);
            result.add(kept);
        }
        result.addAll(saveImages(post, upload(files), keepImageUrls.size()));
        post.updateContent(content);
        return toResponse(post, result, actor);
    }

    // 게시글 삭제 시 사진·댓글·좋아요를 같은 시점에 함께 삭제
    public void deletePost(User actor, Long postId) {
        Post post = getActivePost(postId);
        checkAuthor(post, actor);
        List<String> imageUrls = postImageRepository.findAllByPostOrderBySortOrderAsc(post).stream()
                .map(PostImage::getImageUrl)
                .toList();
        postLikeRepository.deleteAllByPost(post);
        commentRepository.deleteAllRepliesByPost(post);
        commentRepository.deleteAllByPost(post);
        postImageRepository.deleteAllByPost(post);
        postRepository.deleteById(post.getId());
        deleteFilesAfterCommit(imageUrls);
    }

    // 개인 투두는 카테고리 소유자, 팀 투두는 현재 담당자(팀 구성원)만 작성
    private void checkWrite(Todo todo, User actor) {
        Category category = categoryService.getActiveCategory(todo.getCategory().getId());
        boolean allowed = category.isTeamCategory()
                ? todo.getAssignee().getId().equals(actor.getId())
                  && teamAuthorityService.findMember(category.getTeam(), actor).isPresent()
                : category.isOwnedBy(actor);
        if (!allowed) {
            throw new CommonException(PostErrorCode.POST_WRITE_DENIED);
        }
    }

    // 차단 관계가 우선, 이후 연결된 투두의 카테고리 공개 범위 적용
    private void checkView(Post post, User viewer) {
        boolean isAuthor = post.isAuthor(viewer);
        if (!isAuthor && blockService.isBlocked(post.getUser(), viewer)) {
            throw new CommonException(PostErrorCode.POST_ACCESS_DENIED);
        }
        Category category = categoryService.getActiveCategory(post.getTodo().getCategory().getId());
        if (!isAuthor && !categoryAuthorityService.canView(category, viewer)) {
            throw new CommonException(PostErrorCode.POST_ACCESS_DENIED);
        }
    }

    private void checkAuthor(Post post, User actor) {
        if (!post.isAuthor(actor)) {
            throw new CommonException(PostErrorCode.NOT_POST_AUTHOR);
        }
    }

    // 탈퇴한 작성자의 게시글은 존재하지 않는 것으로 처리
    private Post getActivePost(Long postId) {
        return postRepository.findWithAllById(postId)
                .filter(post -> post.getUser().getDeletedAt() == null)
                .orElseThrow(() -> new CommonException(PostErrorCode.POST_NOT_FOUND));
    }

    // 개인 카테고리는 소유자별, 팀 카테고리는 팀별로 열람 가능 여부 판정 (삭제된 팀·탈퇴한 소유자 제외)
    private Set<Long> viewableCategoryIds(User viewer, List<Category> categories) {
        Map<Long, List<Category>> personalByOwner = categories.stream()
                .filter(category -> !category.isTeamCategory() && category.getUser().getDeletedAt() == null)
                .collect(Collectors.groupingBy(category -> category.getUser().getId()));
        Map<Long, List<Category>> teamByTeam = categories.stream()
                .filter(category -> category.isTeamCategory() && category.getTeam().getDeletedAt() == null)
                .collect(Collectors.groupingBy(category -> category.getTeam().getId()));

        Set<Long> ids = new HashSet<>();
        personalByOwner.values().forEach(group -> categoryAuthorityService
                .filterViewablePersonal(group.get(0).getUser(), viewer, group)
                .forEach(category -> ids.add(category.getId())));
        teamByTeam.values().forEach(group -> categoryAuthorityService
                .filterViewableTeam(group.get(0).getTeam(), viewer, group)
                .forEach(category -> ids.add(category.getId())));
        return ids;
    }

    private PostResponse toResponse(Post post, List<PostImage> images, User viewer) {
        return PostResponse.of(
                post,
                images,
                postLikeRepository.countByPost(post),
                commentRepository.countByPost(post),
                postLikeRepository.existsByPostAndUser(post, viewer)
        );
    }

    // 사진, 좋아요·댓글 수, 요청자의 좋아요 여부를 게시글 묶음 단위로 조회
    private Page<PostResponse> toResponses(Page<Post> posts, User viewer) {
        if (posts.isEmpty()) {
            return posts.map(post -> PostResponse.of(post, List.of(), 0, 0, false));
        }
        List<Post> content = posts.getContent();
        Map<Long, List<PostImage>> imagesByPost = postImageRepository.findAllByPostInOrderBySortOrderAsc(content).stream()
                .collect(Collectors.groupingBy(image -> image.getPost().getId()));
        Map<Long, Long> likeCounts = toCountMap(postLikeRepository.countByPostIn(content));
        Map<Long, Long> commentCounts = toCountMap(commentRepository.countByPostIn(content));
        Set<Long> likedIds = postLikeRepository.findLikedPostIds(content, viewer);
        return posts.map(post -> PostResponse.of(
                post,
                imagesByPost.getOrDefault(post.getId(), List.of()),
                likeCounts.getOrDefault(post.getId(), 0L),
                commentCounts.getOrDefault(post.getId(), 0L),
                likedIds.contains(post.getId())
        ));
    }

    private Map<Long, Long> toCountMap(List<PostCount> counts) {
        return counts.stream()
                .collect(Collectors.toMap(PostCount::postId, PostCount::count));
    }

    private List<PostImage> saveImages(Post post, List<String> urls, int startOrder) {
        List<PostImage> images = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            images.add(PostImage.of(post, urls.get(i), startOrder + i));
        }
        return postImageRepository.saveAll(images);
    }

    // 업로드 후 트랜잭션이 롤백되면 업로드한 파일 삭제
    private List<String> upload(List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                urls.add(s3Service.uploadFile(IMAGE_DIR, file));
            }
        } finally {
            deleteFilesOnRollback(List.copyOf(urls));
        }
        return urls;
    }

    private void deleteFilesOnRollback(List<String> urls) {
        if (urls.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteFilesQuietly(urls);
                }
            }
        });
    }

    // DB 반영이 확정된 뒤에만 S3 파일 삭제
    private void deleteFilesAfterCommit(List<String> urls) {
        if (urls.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFilesQuietly(urls);
            }
        });
    }

    private void deleteFilesQuietly(List<String> urls) {
        for (String url : urls) {
            try {
                s3Service.deleteFile(url);
            } catch (Exception e) {
                log.warn("S3 파일 삭제 실패: {}", url, e);
            }
        }
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
        return PageRequest.of(Math.max(page - 1, 0), size);
    }
}
