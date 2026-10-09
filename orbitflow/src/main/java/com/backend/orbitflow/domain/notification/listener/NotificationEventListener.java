package com.backend.orbitflow.domain.notification.listener;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.repository.CategoryRepository;
import com.backend.orbitflow.domain.category.service.CategoryAuthorityService;
import com.backend.orbitflow.domain.comment.repository.CommentRepository;
import com.backend.orbitflow.domain.follow.entity.Follow;
import com.backend.orbitflow.domain.follow.repository.FollowRepository;
import com.backend.orbitflow.domain.item.repository.ItemRepository;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.*;
import com.backend.orbitflow.domain.notification.service.NotificationService;
import com.backend.orbitflow.domain.post.repository.PostRepository;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.repository.TeamMemberRepository;
import com.backend.orbitflow.domain.team.repository.TeamRepository;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.todo.repository.TodoRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

// 대상 활동의 트랜잭션이 커밋된 직후(활동이 성공적으로 생성된 직후) 별도 스레드에서 알림 생성·발송
// 알림 설정은 이벤트 처리 시점의 값을 사용하므로, 설정 변경은 이후 발생하는 활동부터 적용
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final TodoRepository todoRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TeamAuthorityService teamAuthorityService;
    private final CategoryAuthorityService categoryAuthorityService;
    private final ItemRepository itemRepository;

    // ---------- 팔로우 ----------

    // 팔로우 활동·팔로우 요청 활동은 대상 사용자에게
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(FollowCreatedEvent event) {
        followRepository.findWithUsersById(event.followId()).ifPresent(follow -> {
            User follower = follow.getFollower();
            String content = follow.isPending()
                    ? follower.getName() + "님이 팔로우를 요청했습니다."
                    : follower.getName() + "님이 회원님을 팔로우하기 시작했습니다.";
            notificationService.send(follow.getFollowee(), NotificationType.SOCIAL, follower, follow.getId(), null, content);
        });
    }

    // 비밀계정 팔로우 요청이 승인되면 요청자에게 (거절은 고지하지 않음)
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(FollowAcceptedEvent event) {
        followRepository.findWithUsersById(event.followId()).ifPresent(follow -> {
            User followee = follow.getFollowee();
            notificationService.send(follow.getFollower(), NotificationType.SOCIAL, followee, follow.getId(), null,
                    followee.getName() + "님이 팔로우 요청을 수락했습니다.");
        });
    }

    // ---------- 게시글·댓글·좋아요 ----------

    // 팔로우 계정의 게시글 작성 : 알림을 켠 팔로워 중 게시글을 조회할 수 있는 사용자에게
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(PostCreatedEvent event) {
        postRepository.findWithAllById(event.postId()).ifPresent(post -> {
            User author = post.getUser();
            Category category = post.getTodo().getCategory();
            for (User follower : followRepository.findNotifiableFollowers(author)) {
                if (categoryAuthorityService.canView(category, follower)) {
                    notificationService.send(follower, NotificationType.NEWPOST, author, post.getId(), null,
                            author.getName() + "님이 새 게시글을 작성했습니다.");
                }
            }
        });
    }

    // 게시글 댓글 활동은 게시글 작성자에게
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(CommentCreatedEvent event) {
        commentRepository.findWithAllById(event.commentId()).ifPresent(comment -> {
            User commenter = comment.getUser();
            String content = comment.isReply()
                    ? commenter.getName() + "님이 게시글에 답글을 남겼습니다."
                    : commenter.getName() + "님이 게시글에 댓글을 남겼습니다.";
            notificationService.send(comment.getPost().getUser(), NotificationType.NEWCOMMENT, commenter,
                    comment.getPost().getId(), null, content);
        });
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(PostLikedEvent event) {
        postRepository.findWithAllById(event.postId()).ifPresent(post ->
                userRepository.findById(event.likerId()).ifPresent(liker ->
                        notificationService.send(post.getUser(), NotificationType.LIKE, liker, post.getId(), null,
                                liker.getName() + "님이 게시글을 좋아합니다.")
                ));
    }

    // ---------- 투두 ----------

    // 팔로우 계정의 투두 완료 : 알림을 켠 팔로워 중 투두를 조회할 수 있는 사용자에게
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(TodoCompletedEvent event) {
        todoRepository.findWithAllById(event.todoId()).ifPresent(todo ->
                userRepository.findById(event.completerId()).ifPresent(completer -> {
                    for (User follower : followRepository.findNotifiableFollowers(completer)) {
                        if (categoryAuthorityService.canView(todo.getCategory(), follower)) {
                            notificationService.send(follower, NotificationType.TODO_COMPLETED, completer, todo.getId(), null,
                                    completer.getName() + "님이 '" + todo.getName() + "'을(를) 완료했습니다.");
                        }
                    }
                }));
    }

    // ---------- 팀 ----------

    // 팀 가입·탈퇴·역할 변경은 변경 대상 사용자와 팀 관리자에게
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(TeamJoinedEvent event) {
        withTeamAndUser(event.teamId(), event.userId(), (team, user) -> {
            notificationService.send(user, NotificationType.TEAM_JOINED, null, null, team.getUuid(),
                    "'" + team.getName() + "' 팀에 가입했습니다.");
            notifyAdmins(team, user, NotificationType.TEAM_JOINED,
                    user.getName() + "님이 '" + team.getName() + "' 팀에 가입했습니다.");
        });
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(TeamLeftEvent event) {
        withTeamAndUser(event.teamId(), event.userId(), (team, user) -> {
            notificationService.send(user, NotificationType.TEAM_LEFT, null, null, team.getUuid(),
                    event.kicked()
                            ? "'" + team.getName() + "' 팀에서 내보내졌습니다."
                            : "'" + team.getName() + "' 팀에서 탈퇴했습니다.");
            notifyAdmins(team, user, NotificationType.TEAM_LEFT,
                    user.getName() + "님이 '" + team.getName() + "' 팀에서 " + (event.kicked() ? "내보내졌습니다." : "탈퇴했습니다."));
        });
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(TeamRoleChangedEvent event) {
        withTeamAndUser(event.teamId(), event.userId(), (team, user) -> {
            notificationService.send(user, NotificationType.TEAM_ROLE_CHANGED, null, null, team.getUuid(),
                    "'" + team.getName() + "' 팀에서 회원님의 역할이 변경되었습니다.");
            notifyAdmins(team, user, NotificationType.TEAM_ROLE_CHANGED,
                    "'" + team.getName() + "' 팀에서 " + user.getName() + "님의 역할이 변경되었습니다.");
        });
    }

    // 팀 카테고리 공개 범위 변경 : 변경 전 또는 변경 후에 조회 권한이 있는 팀원에게 (변경한 본인 제외)
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(CategoryVisibilityChangedEvent event) {
        categoryRepository.findWithOwnerById(event.categoryId())
                .filter(Category::isTeamCategory)
                .ifPresent(category -> {
                    Team team = category.getTeam();
                    User actor = userRepository.findById(event.actorId()).orElse(null);
                    Map<Long, User> receivers = new LinkedHashMap<>();
                    for (TeamMember member : teamMemberRepository.findAllWithUserByTeam(team)) {
                        User user = member.getUser();
                        if (event.viewerIdsBefore().contains(user.getId()) || categoryAuthorityService.canView(category, user)) {
                            receivers.put(user.getId(), user);
                        }
                    }
                    receivers.values().forEach(user -> notificationService.send(
                            user, NotificationType.CATEGORY_VISIBILITY_CHANGED, actor, category.getId(), team.getUuid(),
                            "'" + team.getName() + "' 팀의 '" + category.getName() + "' 카테고리 공개 범위가 "
                                    + category.getVisibility() + "(으)로 변경되었습니다."
                    ));
                });
    }

    // ---------- 포인트·아이템 ----------

    // 포인트 적립·꾸밈 요소 구매 결과는 처리 완료 즉시 앱 내 알림으로 고지
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(PointEarnedEvent event) {
        userRepository.findById(event.userId()).ifPresent(user ->
                notificationService.send(user, NotificationType.POINT_EARNED, null, null, null,
                        event.reason() + " " + event.amount() + "P가 적립되었습니다. (잔액 " + event.balanceAfter() + "P)"));
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(PointRevokedEvent event) {
        userRepository.findById(event.userId()).ifPresent(user ->
                notificationService.send(user, NotificationType.POINT_REVOKED, null, null, null,
                        "무효 처리된 출석 포인트 " + event.amount() + "P가 회수되었습니다. (잔액 " + event.balanceAfter() + "P)"));
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(ItemPurchasedEvent event) {
        userRepository.findById(event.userId()).ifPresent(user ->
                itemRepository.findById(event.itemId()).ifPresent(item ->
                        notificationService.send(user, NotificationType.ITEM_PURCHASED, null, item.getId(), null,
                                "'" + item.getName() + "'을(를) " + event.price() + "P에 구매했습니다. (잔액 " + event.balanceAfter() + "P)")));
    }

    // 팀 관리자 : 소유자 또는 MANAGE_TEAM 권한 보유자 (변경 대상 본인 제외)
    private void notifyAdmins(Team team, User subject, NotificationType type, String content) {
        Set<Long> notified = new HashSet<>();
        notified.add(subject.getId());
        List<TeamMember> members = teamMemberRepository.findAllWithUserByTeam(team);
        for (TeamMember member : members) {
            User user = member.getUser();
            if (notified.contains(user.getId())) {
                continue;
            }
            if (TeamPermission.MANAGE_TEAM.isGranted(teamAuthorityService.getPermissionMask(team, member))) {
                notified.add(user.getId());
                notificationService.send(user, type, subject, null, team.getUuid(), content);
            }
        }
    }

    private void withTeamAndUser(Long teamId, Long userId, BiConsumer<Team, User> action) {
        teamRepository.findById(teamId)
                .filter(team -> team.getDeletedAt() == null)
                .ifPresent(team -> userRepository.findById(userId).ifPresent(user -> action.accept(team, user)));
    }
}
