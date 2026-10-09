package com.backend.orbitflow.domain.timeline.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.service.CategoryAuthorityService;
import com.backend.orbitflow.domain.follow.repository.FollowRepository;
import com.backend.orbitflow.domain.post.dto.response.PostResponse;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.post.repository.PostRepository;
import com.backend.orbitflow.domain.post.service.PostService;
import com.backend.orbitflow.domain.timeline.dto.TimelineCursor;
import com.backend.orbitflow.domain.timeline.dto.TimelineResponse;
import com.backend.orbitflow.domain.timeline.enums.TimelineItemType;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.repository.TodoRepository;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 타임라인 : 팔로우한 계정의 투두 기반 게시글과 투두 완료 활동 (조회 시점 병합, 별도 저장 없음)
 *
 * <p>두 출처(게시글, 투두 완료)를 정렬 키 (발생 시각 desc, 종류, id desc)로 병합하면서
 * 열람 권한(카테고리 공개 범위)이 없는 항목은 건너뛰고, 페이지가 찰 때까지 필요한 만큼만 추가 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimelineService {

    public static final int MAX_SIZE = 50;
    // 출처별 1회 조회 묶음 크기
    private static final int BATCH_SIZE = 30;
    // 열람 불가 항목이 많을 때 한 요청에서 검사할 최대 항목 수 (넘으면 그 위치부터 다음 페이지로)
    private static final int MAX_EXAMINED = 500;

    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final TodoRepository todoRepository;
    private final PostService postService;
    private final CategoryAuthorityService categoryAuthorityService;

    public TimelineResponse getTimeline(User viewer, String cursorValue, int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_SIZE);
        TimelineCursor cursor = TimelineCursor.decode(cursorValue);
        List<User> followees = followRepository.findAcceptedFollowees(viewer);
        if (followees.isEmpty()) {
            return new TimelineResponse(List.of(), null, false);
        }

        Source<Post> posts = new Source<>(cursor, TimelineItemType.POST,
                c -> postRepository.findTimelinePosts(followees, c.time(), c.kind(), c.id(), PageRequest.of(0, BATCH_SIZE)),
                Post::getCreatedAt, Post::getId);
        Source<Todo> todos = new Source<>(cursor, TimelineItemType.TODO_COMPLETED,
                c -> todoRepository.findTimelineCompletedTodos(followees, c.time(), c.kind(), c.id(), PageRequest.of(0, BATCH_SIZE)),
                Todo::getCompletedAt, Todo::getId);

        Map<Long, Boolean> viewableByCategory = new HashMap<>();
        List<Candidate> picked = new ArrayList<>();
        Candidate lastExamined = null;
        int examined = 0;

        // 페이지 크기 + 1개를 모아 다음 페이지 존재 여부 판단
        while (picked.size() <= pageSize && examined < MAX_EXAMINED) {
            Candidate next = pickNext(posts.peek(), todos.peek());
            if (next == null) {
                break;
            }
            (next.type() == TimelineItemType.POST ? posts : todos).poll();
            examined++;
            lastExamined = next;
            if (isViewable(next.category(), viewer, viewableByCategory)) {
                picked.add(next);
            }
        }

        boolean hasMorePicked = picked.size() > pageSize;
        List<Candidate> items = hasMorePicked ? picked.subList(0, pageSize) : picked;
        boolean cutByLimit = !hasMorePicked && examined >= MAX_EXAMINED && (posts.peek() != null || todos.peek() != null);
        boolean hasNext = hasMorePicked || cutByLimit;
        String nextCursor = null;
        if (hasMorePicked) {
            nextCursor = items.get(items.size() - 1).cursor().encode();
        } else if (cutByLimit) {
            nextCursor = lastExamined.cursor().encode();
        }
        return new TimelineResponse(toItems(items, viewer), nextCursor, hasNext);
    }

    private Candidate pickNext(Candidate post, Candidate todo) {
        if (post == null) {
            return todo;
        }
        if (todo == null) {
            return post;
        }
        return post.cursor().isBefore(todo.cursor()) ? post : todo;
    }

    // 카테고리 공개 범위 판정 (요청 내 캐시), 팀이 삭제됐거나 소유자가 탈퇴한 카테고리는 제외
    private boolean isViewable(Category category, User viewer, Map<Long, Boolean> cache) {
        return cache.computeIfAbsent(category.getId(), id -> {
            boolean ownerActive = category.isTeamCategory()
                    ? category.getTeam().getDeletedAt() == null
                    : category.getUser().getDeletedAt() == null;
            return ownerActive && categoryAuthorityService.canView(category, viewer);
        });
    }

    private List<TimelineResponse.Item> toItems(List<Candidate> candidates, User viewer) {
        List<Post> posts = candidates.stream()
                .filter(candidate -> candidate.type() == TimelineItemType.POST)
                .map(candidate -> (Post) candidate.source())
                .toList();
        Map<Long, PostResponse> postResponses = new HashMap<>();
        postService.toResponses(posts, viewer).forEach(response -> postResponses.put(response.id(), response));

        return candidates.stream()
                .map(candidate -> {
                    if (candidate.type() == TimelineItemType.POST) {
                        Post post = (Post) candidate.source();
                        return new TimelineResponse.Item(candidate.type(), candidate.cursor().time(), actor(post.getUser()),
                                postResponses.get(post.getId()), null);
                    }
                    Todo todo = (Todo) candidate.source();
                    return new TimelineResponse.Item(candidate.type(), candidate.cursor().time(), actor(todo.getAssignee()),
                            null, TodoResponse.from(todo));
                })
                .toList();
    }

    private TimelineResponse.Actor actor(User user) {
        return new TimelineResponse.Actor(user.getUuid(), user.getName(), user.getProfileImage());
    }

    private record Candidate(TimelineItemType type, TimelineCursor cursor, Category category, Object source) {
    }

    // 출처별 버퍼 : 비면 마지막 위치 다음부터 BATCH_SIZE만큼 추가 조회
    private final class Source<T> {

        private final Deque<Candidate> buffer = new ArrayDeque<>();
        private final TimelineItemType type;
        private final Function<TimelineCursor, List<T>> fetcher;
        private final Function<T, LocalDateTime> timeOf;
        private final Function<T, Long> idOf;
        private TimelineCursor position;
        private boolean exhausted;

        Source(TimelineCursor start, TimelineItemType type, Function<TimelineCursor, List<T>> fetcher,
               Function<T, LocalDateTime> timeOf, Function<T, Long> idOf) {
            this.position = start;
            this.type = type;
            this.fetcher = fetcher;
            this.timeOf = timeOf;
            this.idOf = idOf;
        }

        Candidate peek() {
            if (buffer.isEmpty() && !exhausted) {
                fill();
            }
            return buffer.peekFirst();
        }

        void poll() {
            buffer.pollFirst();
        }

        private void fill() {
            List<T> rows = fetcher.apply(position);
            if (rows.size() < BATCH_SIZE) {
                exhausted = true;
            }
            for (T row : rows) {
                TimelineCursor key = new TimelineCursor(timeOf.apply(row), type.getOrder(), idOf.apply(row));
                buffer.addLast(new Candidate(type, key, categoryOf(row), row));
                position = key;
            }
        }

        private Category categoryOf(T row) {
            return row instanceof Post post ? post.getTodo().getCategory() : ((Todo) row).getCategory();
        }
    }
}
