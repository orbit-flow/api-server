package com.backend.orbitflow.domain.todo.facade;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.category.service.CategoryAuthorityService;
import com.backend.orbitflow.domain.category.service.CategoryService;
import com.backend.orbitflow.domain.follow.enums.FollowState;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.dto.NotificationDraft;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.notification.service.NotificationService;
import com.backend.orbitflow.domain.todo.reminder.TodoReminderService;
import com.backend.orbitflow.domain.todo.dto.request.RoutineOccurrenceRequest;
import com.backend.orbitflow.domain.todo.dto.request.RoutineRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoAssigneeRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoCreateRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoOrderRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoUpdateRequest;
import com.backend.orbitflow.domain.todo.dto.response.RoutineResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.entity.Routine;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.enums.RoutineScope;
import com.backend.orbitflow.domain.todo.service.RoutineService;
import com.backend.orbitflow.domain.todo.service.TodoAuthorityService;
import com.backend.orbitflow.domain.todo.service.TodoService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.backend.orbitflow.domain.todo.dto.response.DashboardTodoResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;

@Component
@RequiredArgsConstructor
public class TodoFacade {

    private final TodoService todoService;
    private final RoutineService routineService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final CategoryAuthorityService categoryAuthorityService;
    private final TeamAuthorityService teamAuthorityService;
    private final BlockService blockService;
    private final FollowService followService;
    private final TodoAuthorityService todoAuthorityService;
    private final TodoReminderService todoReminderService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    // 스케줄러(TodoScheduler)가 호출 : 발송 대상 조회와 알림 저장은 한 트랜잭션 (같은 분에 도래한 리마인드를 INSERT 1회로 저장)
    @Transactional
    public int sendReminders(List<Long> todoIds, LocalDateTime now) {
        List<NotificationDraft> drafts = todoReminderService.createDrafts(todoIds, now);
        notificationService.sendEach(drafts);
        return drafts.size();
    }

    // 팀 투두를 다른 구성원에게 배정해 만들면 담당자에게 알림
    @Transactional
    public TodoResponse createTodo(AuthUser authUser, Long categoryId, TodoCreateRequest request) {
        User me = me(authUser);
        User assignee = request.assigneeUuid() == null ? null : userService.getByUuid(request.assigneeUuid());
        Category category = categoryService.getActiveCategory(categoryId);
        Integer teamMask = teamMask(category, me);
        todoAuthorityService.checkEdit(category, me, teamMask);
        todoService.validatePeriod(request.startDate(), request.endDate());

        Todo parent = null;
        if (request.parentTodoId() != null) {
            parent = activeTodo(request.parentTodoId());
            todoService.validateParent(parent, category, request.routine());
        }

        User todoAssignee = todoService.resolveAssignee(category, me, assignee);
        todoAuthorityService.checkAssignOnCreate(category, me, todoAssignee, teamMask, isTeamMember(category, todoAssignee));

        Todo todo = todoService.createTodo(
                category,
                parent,
                todoAssignee,
                request.type(),
                request.name(),
                request.startDate(),
                request.endDate(),
                request.remindBeforeMinutes()
        );
        RoutineRequest routine = request.routine();
        if (routine != null) {
            routineService.upsertRoutine(todo, routine.durationType(), routine.duration(), routine.daysOfWeek(), routine.repeatEndDate(), routine.scope());
        }
        if (category.isTeamCategory()) {
            notifyAssigned(todo, me);
        }
        return todoService.getTodo(todo);
    }

    @Transactional(readOnly = true)
    public PageResponse<TodoResponse> getCategoryTodos(AuthUser authUser, Long categoryId, int page, int size) {
        User me = me(authUser);
        Category category = categoryService.getActiveCategory(categoryId);
        todoAuthorityService.checkView(canView(category, me));
        return PageResponse.from(todoService.getCategoryTodos(category, page, size));
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodo(AuthUser authUser, Long todoId) {
        User me = me(authUser);
        Todo todo = activeTodo(todoId);
        todoAuthorityService.checkView(canView(todo.getCategory(), me));
        return todoService.getTodo(todo);
    }

    @Transactional(readOnly = true)
    public PageResponse<DashboardTodoResponse> getDashboardSchedules(AuthUser authUser, LocalDate date, int page, int size) {
        return PageResponse.from(todoService.getDashboardSchedules(me(authUser), dateOrToday(date), page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<DashboardTodoResponse> getDashboardBacklogs(AuthUser authUser, LocalDate date, int page, int size) {
        return PageResponse.from(todoService.getDashboardBacklogs(me(authUser), dateOrToday(date), page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<DashboardTodoResponse> getDashboardPreviews(AuthUser authUser, LocalDate date, int page, int size) {
        return PageResponse.of(routineService.getDashboardPreviews(me(authUser), dateOrToday(date)), page, size);
    }

    private LocalDate dateOrToday(LocalDate date) {
        return date == null ? LocalDate.now() : date;
    }

    @Transactional
    public TodoResponse updateTodo(AuthUser authUser, Long todoId, TodoUpdateRequest request) {
        User me = me(authUser);
        Todo todo = activeTodo(todoId);
        todoAuthorityService.checkEdit(todo.getCategory(), me, teamMask(todo.getCategory(), me));
        return todoService.updateTodo(
                todo,
                request.type(),
                request.name(),
                request.startDate(),
                request.endDate(),
                request.remindBeforeMinutes()
        );
    }

    // 완료 처리 시 알림을 켠 팔로워 중 투두를 조회할 수 있는 사용자에게 알림 (완료 취소는 알림 없음)
    @Transactional
    public TodoResponse toggleComplete(AuthUser authUser, Long todoId) {
        User me = me(authUser);
        Todo todo = activeTodo(todoId);
        todoAuthorityService.checkComplete(todo, me, teamMask(todo.getCategory(), me));
        TodoResponse response = todoService.toggleComplete(todo);
        if (response.isCompleted()) {
            findNotifiableViewers(todo.getCategory(), me).forEach(receivers ->
                    eventPublisher.publishEvent(new NotificationRequest(receivers, NotificationType.TODO_COMPLETED, me, todo.getId(), null,
                            me.getName() + "님이 '" + todo.getName() + "'을(를) 완료했습니다.")));
        }
        return response;
    }

    // 담당자가 바뀌면 새 담당자에게 알림
    @Transactional
    public TodoResponse updateAssignee(AuthUser authUser, Long todoId, TodoAssigneeRequest request) {
        User me = me(authUser);
        User assignee = userService.getByUuid(request.userUuid());
        Todo todo = activeTodo(todoId);
        todoService.requireTeamTodo(todo);
        Category category = todo.getCategory();
        todoAuthorityService.checkAssign(teamMask(category, me), isTeamMember(category, assignee));
        Long previousAssigneeId = todo.getAssignee().getId();
        TodoResponse response = todoService.updateAssignee(todo, assignee);
        if (!todo.getAssignee().getId().equals(previousAssigneeId)) {
            notifyAssigned(todo, me);
        }
        return response;
    }

    @Transactional
    public void updateChildOrder(AuthUser authUser, Long todoId, TodoOrderRequest request) {
        User me = me(authUser);
        Todo parent = activeTodo(todoId);
        todoAuthorityService.checkEdit(parent.getCategory(), me, teamMask(parent.getCategory(), me));
        todoService.updateChildOrder(parent, request.todoIds());
    }

    @Transactional
    public void deleteTodo(AuthUser authUser, Long todoId) {
        User me = me(authUser);
        Todo todo = activeTodo(todoId);
        todoAuthorityService.checkEdit(todo.getCategory(), me, teamMask(todo.getCategory(), me));
        todoService.deleteTodo(todo);
    }

    @Transactional(readOnly = true)
    public PageResponse<TodoResponse> getRestorableTodos(AuthUser authUser, Long categoryId, int page, int size) {
        User me = me(authUser);
        Category category = categoryService.getActiveCategory(categoryId);
        todoAuthorityService.checkEdit(category, me, teamMask(category, me));
        return PageResponse.from(todoService.getRestorableTodos(category, page, size));
    }

    @Transactional
    public TodoResponse restoreTodo(AuthUser authUser, Long todoId) {
        User me = me(authUser);
        Todo todo = todoService.getRestorableTodo(todoId);
        Category category = categoryService.getActiveCategory(todo.getCategory().getId());
        todoAuthorityService.checkEdit(category, me, teamMask(category, me));
        return todoService.restoreTodo(todo);
    }

    @Transactional
    public RoutineResponse upsertRoutine(AuthUser authUser, Long todoId, RoutineRequest request) {
        // 반복 설정은 반복에 속한 투두(원본·회차) 어디서든 가능, 반복 중이면 변경 범위(scope)를 함께 받음
        User me = me(authUser);
        Todo todo = activeTodo(todoId);
        todoAuthorityService.checkEdit(todo.getCategory(), me, teamMask(todo.getCategory(), me));
        return routineService.upsertRoutine(todo, request.durationType(), request.duration(), request.daysOfWeek(), request.repeatEndDate(), request.scope());
    }

    @Transactional
    public void deleteRoutine(AuthUser authUser, Long todoId, RoutineScope scope) {
        User me = me(authUser);
        Todo todo = activeTodo(todoId);
        todoAuthorityService.checkEdit(todo.getCategory(), me, teamMask(todo.getCategory(), me));
        routineService.deleteRoutine(todo, scope);
    }

    @Transactional
    public TodoResponse createOccurrence(AuthUser authUser, Long routineId, RoutineOccurrenceRequest request) {
        User me = me(authUser);
        Routine routine = routineService.getRoutine(routineId);
        Category category = routine.getTodo().getCategory();
        todoAuthorityService.checkEdit(category, me, teamMask(category, me));
        return routineService.createOccurrence(routine, request.startDate());
    }

    // 팀 투두 배정 : 본인에게 배정한 경우는 제외
    private void notifyAssigned(Todo todo, User actor) {
        User assignee = todo.getAssignee();
        if (assignee.getId().equals(actor.getId())) {
            return;
        }
        eventPublisher.publishEvent(NotificationRequest.to(assignee, NotificationType.TODO_ASSIGNED, actor, todo.getId(), null,
                actor.getName() + "님이 '" + todo.getName() + "'을(를) 회원님에게 배정했습니다."));
    }

    // 소유자가 탈퇴했거나 팀이 삭제된 카테고리의 투두도 존재하지 않는 것으로 처리
    private Todo activeTodo(Long todoId) {
        Todo todo = todoService.getActiveTodo(todoId);
        categoryService.getActiveCategory(todo.getCategory().getId());
        return todo;
    }

    // 팀 카테고리에서의 권한 mask (팀 구성원이 아니면 null, 개인 카테고리는 팀 권한을 보지 않으므로 조회하지 않음)
    private Integer teamMask(Category category, User user) {
        if (!category.isTeamCategory()) {
            return null;
        }
        Team team = category.getTeam();
        return teamAuthorityService.findMember(team, user)
                .map(member -> teamAuthorityService.getPermissionMask(team, member))
                .orElse(null);
    }

    private boolean isTeamMember(Category category, User user) {
        return category.isTeamCategory() && teamAuthorityService.findMember(category.getTeam(), user).isPresent();
    }

    // 카테고리 열람 가능 여부 : 소유 도메인(팀·차단·팔로우)의 정보를 조회해 정책에 전달
    private boolean canView(Category category, User viewer) {
        if (category.isTeamCategory()) {
            Team team = category.getTeam();
            TeamMember member = teamAuthorityService.findMember(team, viewer).orElse(null);
            Integer mask = member == null ? null : teamAuthorityService.getPermissionMask(team, member);
            return categoryAuthorityService.canViewTeam(category, member, mask);
        }
        User owner = category.getUser();
        // 본인 카테고리는 관계를 조회할 필요가 없고, 차단 관계면 팔로우 상태는 판정에 쓰이지 않음
        boolean self = owner.getId().equals(viewer.getId());
        boolean blocked = !self && blockService.isBlocked(owner, viewer);
        FollowState followState = self || blocked ? null : followService.getFollowState(viewer, owner);
        return categoryAuthorityService.canViewPersonal(category, viewer, blocked, followState);
    }

    // 알림을 켠 followee의 팔로워 중 카테고리를 볼 수 있는 사용자 (id → uuid, 팔로워 FOLLOWER_CHUNK명 단위 묶음)
    private List<Map<Long, String>> findNotifiableViewers(Category category, User followee) {
        List<Map<Long, String>> chunks = new ArrayList<>();
        if (!categoryAuthorityService.hasNotifiableFollowers(category)) {
            return chunks;
        }
        Set<Long> viewerIds = categoryAuthorityService.findNotifiableViewerIds(category);
        long afterId = 0L;
        while (true) {
            Map<Long, String> followers = followService.findNotifiableFollowers(followee, afterId, CategoryAuthorityService.FOLLOWER_CHUNK);
            for (Long followerId : followers.keySet()) {
                afterId = followerId;
            }
            Map<Long, String> receivers = categoryAuthorityService.filterNotifiableViewers(followers, viewerIds);
            if (!receivers.isEmpty()) {
                chunks.add(receivers);
            }
            if (followers.size() < CategoryAuthorityService.FOLLOWER_CHUNK) {
                return chunks;
            }
        }
    }

    private User me(AuthUser authUser) {
        return userService.getActiveByUuid(authUser.getUuid());
    }
}
