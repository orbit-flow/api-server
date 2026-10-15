package com.backend.orbitflow.domain.purge.service;

import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.util.S3Service;
import com.backend.orbitflow.global.util.S3TransactionalFileManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 논리적 삭제 후 30일이 지난 데이터 영구 삭제
 *
 * <ul>
 *   <li>탈퇴 사용자 : 식별정보를 파기(익명화)하고 종속 데이터 삭제
 *       - 사용자 행은 결제·포인트 거래(법정 보관)·정지·신고 기록의 참조를 위해 익명 상태로 유지되므로
 *         DB의 ON DELETE CASCADE가 동작하지 않아 종속 데이터를 직접 삭제 (각 삭제의 하위 데이터는 DB가 연쇄 삭제)</li>
 *   <li>삭제된 팀 : 팀 행 삭제, 팀 채팅방·카테고리(투두·게시글)·역할·구성원·초대는 DB가 연쇄 삭제</li>
 *   <li>삭제된 투두 : 연결된 게시글이 없는 투두만 삭제 (게시글은 "삭제된 투두"로 표시되며 유지)</li>
 * </ul>
 *
 * <p>대상 단위(사용자·팀)마다 별도 트랜잭션으로 처리해 한 건의 실패가 전체를 막지 않음 (DataPurgeScheduler가 건별로 호출하고 실패를 기록)
 * <p>S3 파일은 연쇄 삭제 전에 목록을 수집하고 커밋 후 삭제
 * <p>탈퇴 사용자는 투두 상속(TodoTransferService)이 중간에 필요해 DataPurgeFacade가 사용자 단위 트랜잭션 안에서 단계별 메서드를 조율
 */
@Service
@RequiredArgsConstructor
public class DataPurgeService {

    public static final int RETENTION_DAYS = 30;
    private static final String WITHDRAWN_EMAIL_PREFIX = "withdrawn-";
    private static final int TODO_BATCH_SIZE = 500;

    private final EntityManager em;
    private final S3Service s3Service;
    private final S3TransactionalFileManager s3FileManager;
    private final ApplicationEventPublisher eventPublisher;

    // ---------- 탈퇴 사용자 ----------

    @Transactional(readOnly = true)
    public List<Long> findWithdrawnUserIds(LocalDateTime threshold) {
        return em.createQuery("""
                        select u.id from User u
                        where u.deletedAt is not null and u.deletedAt < :threshold
                          and u.email not like :prefix
                        """, Long.class)
                .setParameter("threshold", threshold)
                .setParameter("prefix", WITHDRAWN_EMAIL_PREFIX + "%")
                .getResultList();
    }

    // 사용자 단위 영구 삭제는 DataPurgeFacade가 하나의 트랜잭션에서 1단계 -> 팀별 투두 상속 -> 3단계 순으로 호출
    // 1단계 : 목록 수집 이후 로그인으로 복구된 사용자를 지우지 않도록 락을 잡고 대상 조건을 다시 확인 (대상이 아니면 empty)
    public Optional<User> lockPurgeTarget(Long userId) {
        User user = em.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null || !isPurgeTarget(user.getDeletedAt()) || (user.getEmail() != null && user.getEmail().startsWith(WITHDRAWN_EMAIL_PREFIX))) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    // 2단계 준비 : 팀 소속 해제 대상 팀 (담당 미완료 팀 투두는 호출 측이 팀 탈퇴와 같은 규칙으로 상속, 완료 투두는 익명 사용자로 유지)
    public List<Team> findJoinedTeams(User user) {
        return em.createQuery("select m.team from TeamMember m where m.user = :user", Team.class)
                .setParameter("user", user)
                .getResultList();
    }

    // 3단계 : 나머지 종속 데이터 삭제와 식별정보 파기 (투두 상속 이후 호출)
    public void purgeUserData(User user) {
        Long userId = user.getId();
        Map<String, Object> byUser = Map.of("user", user);

        // 연쇄 삭제될 게시글 사진 (작성한 게시글 + 개인 투두에 달린 게시글)
        List<String> files = new ArrayList<>(strings("""
                select pi.imageUrl from PostImage pi
                where pi.post.user = :user or pi.post.todo.category.user = :user
                """, byUser));
        if (s3Service.isManagedFile(user.getProfileImage())) {
            files.add(user.getProfileImage());
        }

        // 팀 소속 해제 (투두 상속은 완료된 상태)
        update("delete from TeamMember m where m.user = :user", byUser);                 // 역할 부여·카테고리 권한 연쇄
        update("delete from TeamInvitation i where i.inviter = :user or i.invitee = :user", byUser);

        // 콘텐츠 (하위 데이터는 DB 연쇄 삭제)
        update("delete from Post p where p.user = :user", byUser);                       // 사진·댓글·좋아요
        update("delete from Comment c where c.user = :user", byUser);                    // 대댓글
        update("delete from PostLike l where l.user = :user", byUser);
        update("delete from Message m where m.sender = :user", byUser);                  // 메시지 숨김
        update("delete from Category c where c.user = :user", byUser);                   // 개인 투두·반복·게시글

        // 관계·참여·알림·소셜 연결·아바타
        update("delete from MessageHide h where h.user = :user", byUser);
        update("delete from ChatroomMember cm where cm.user = :user", byUser);
        update("delete from Follow f where f.follower = :user or f.followee = :user", byUser);
        update("delete from Block b where b.blocker = :user or b.blockee = :user", byUser);
        update("delete from Notification n where n.user = :user or n.actor = :user", byUser);
        update("delete from OAuthAccount o where o.user = :user", byUser);
        update("delete from Avatar a where a.user = :user", byUser);                     // 보유 아이템 연쇄
        // 포인트 거래·결제·정지·신고 기록은 법정 보관·운영 이력으로 유지 (FK에 CASCADE 미선언)

        // 식별정보 파기
        em.clear();
        em.find(User.class, userId).anonymize(WITHDRAWN_EMAIL_PREFIX + user.getUuid() + "@orbitflow.invalid");
        s3FileManager.deleteAfterCommit(files);
    }

    // 회원 탈퇴 후 영구 삭제로 상속된 투두 : 상속받은 구성원에게 팀별로 1건 (탈퇴한 사용자는 익명 처리)
    public void notifyInherited(Team team, Map<Long, Integer> inherited) {
        inherited.forEach((heirId, count) -> {
            User heir = em.find(User.class,  heirId);
            if (heir != null) {
                eventPublisher.publishEvent(NotificationRequest.to(heir, NotificationType.TODO_ASSIGNED, null, null, team.getUuid(),
                        "'" + team.getName() + "' 팀을 떠난 탈퇴한 사용자님이 담당하던 투두 " + count + "개가 회원님에게 배정되었습니다."));
            }
        });
    }

    // ---------- 삭제된 팀 ----------

    @Transactional(readOnly = true)
    public List<Long> findPurgeTeamIds(LocalDateTime threshold) {
        return em.createQuery(
                        "select t.id from Team t where t.deletedAt is not null and t.deletedAt < :threshold", Long.class)
                .setParameter("threshold", threshold)
                .getResultList();
    }

    // 팀 단위 트랜잭션 : 팀 행 삭제 시 채팅방(메시지)·카테고리(투두·게시글)·역할·구성원·초대는 DB가 연쇄 삭제
    @Transactional
    public void purgeTeam(Long teamId) {
        // 목록 수집 이후 복구된 팀은 삭제하지 않음 (락을 잡고 대상 조건을 다시 확인)
        Team team = em.find(Team.class, teamId, LockModeType.PESSIMISTIC_WRITE);
        if (team == null || !isPurgeTarget(team.getDeletedAt())) {
            return;
        }
        List<String> files = strings(
                "select pi.imageUrl from PostImage pi where pi.post.todo.category.team.id = :teamId",
                Map.of("teamId", teamId));
        update("delete from Team t where t.id = :teamId", Map.of("teamId", teamId));
        s3FileManager.deleteAfterCommit(files);
    }

    // ---------- 삭제된 투두 ----------

    // 한 묶음(TODO_BATCH_SIZE) 단위 트랜잭션, 삭제한 수를 반환 (스케줄러가 0이 될 때까지 반복 호출 : 하위부터 여러 차례에 걸쳐 삭제)
    // 게시글이 연결된 투두와, 하위 투두가 남아 있는 투두는 제외
    // 반복 규칙의 원본 투두도 제외 : 원본 삭제는 그 회차만 삭제한 것이고 규칙은 원본을 기준으로 계속 회차를 만듦 (규칙을 해제하면 삭제 대상)
    @Transactional
    public int purgeTodoBatch(LocalDateTime threshold) {
        List<Long> todoIds = em.createQuery("""
                        select t.id from Todo t
                        where t.deletedAt is not null and t.deletedAt < :threshold
                          and not exists (select p.id from Post p where p.todo = t)
                          and not exists (select c.id from Todo c where c.parentTodo = t)
                          and not exists (select r.id from Routine r where r.todo = t)
                        """, Long.class)
                .setParameter("threshold", threshold)
                .setMaxResults(TODO_BATCH_SIZE)
                .getResultList();
        if (!todoIds.isEmpty()) {
            update("delete from Todo t where t.id in :ids", Map.of("ids", todoIds));
        }
        return todoIds.size();
    }

    // ---------- 공통 ----------

    // 목록 수집 시점의 기준과 같은 보관 기간 조건 (복구되어 deletedAt이 null이거나 보관 기간 이내면 제외)
    private boolean isPurgeTarget(LocalDateTime deletedAt) {
        return deletedAt != null && deletedAt.isBefore(LocalDateTime.now().minusDays(RETENTION_DAYS));
    }

    private List<String> strings(String query, Map<String, Object> params) {
        var typed = em.createQuery(query, String.class);
        params.forEach(typed::setParameter);
        return typed.getResultList();
    }

    private void update(String query, Map<String, Object> params) {
        var typed = em.createQuery(query);
        params.forEach(typed::setParameter);
        typed.executeUpdate();
    }
}
