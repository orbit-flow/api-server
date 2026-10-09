package com.backend.orbitflow.domain.purge.service;

import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.util.S3Service;
import com.backend.orbitflow.global.util.S3TransactionalFileManager;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
 * <p>대상 단위(사용자·팀)마다 별도 트랜잭션으로 처리해 한 건의 실패가 전체를 막지 않음
 * <p>S3 파일은 연쇄 삭제 전에 목록을 수집하고 커밋 후 삭제
 */
@Slf4j
@Service
public class DataPurgeService {

    public static final int RETENTION_DAYS = 30;
    private static final String WITHDRAWN_EMAIL_PREFIX = "withdrawn-";
    private static final int TODO_BATCH_SIZE = 500;
    private static final int MAX_TODO_ROUNDS = 20;

    private final EntityManager em;
    private final TransactionTemplate transactionTemplate;
    private final S3Service s3Service;
    private final S3TransactionalFileManager s3FileManager;

    public DataPurgeService(EntityManager em, PlatformTransactionManager transactionManager,
                            S3Service s3Service, S3TransactionalFileManager s3FileManager) {
        this.em = em;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.s3Service = s3Service;
        this.s3FileManager = s3FileManager;
    }

    public void purgeAll() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int teams = purgeTeams(threshold);
        int users = purgeWithdrawnUsers(threshold);
        int todos = purgeTodos(threshold);
        log.info("영구 삭제 완료 : 팀 {}건, 탈퇴 사용자 {}건, 투두 {}건", teams, users, todos);
    }

    // ---------- 탈퇴 사용자 ----------

    public int purgeWithdrawnUsers(LocalDateTime threshold) {
        List<Long> userIds = transactionTemplate.execute(s -> em.createQuery("""
                        select u.id from User u
                        where u.deletedAt is not null and u.deletedAt < :threshold
                          and u.email not like :prefix
                        """, Long.class)
                .setParameter("threshold", threshold)
                .setParameter("prefix", WITHDRAWN_EMAIL_PREFIX + "%")
                .getResultList());
        return runEach(userIds, "탈퇴 사용자", this::purgeUser);
    }

    private void purgeUser(Long userId) {
        User user = em.find(User.class, userId);
        Map<String, Object> byUser = Map.of("user", user);

        // 연쇄 삭제될 게시글 사진 (작성한 게시글 + 개인 투두에 달린 게시글)
        List<String> files = new ArrayList<>(strings("""
                select pi.imageUrl from PostImage pi
                where pi.post.user = :user or pi.post.todo.category.user = :user
                """, byUser));
        if (s3Service.isManagedFile(user.getProfileImage())) {
            files.add(user.getProfileImage());
        }

        // 팀 소속 해제 : 담당 미완료 팀 투두는 팀 소유자가 상속 (팀 탈퇴와 동일), 완료 투두는 익명 사용자로 유지
        List<TeamMember> memberships = em.createQuery(
                        "select m from TeamMember m join fetch m.team where m.user = :user", TeamMember.class)
                .setParameter("user", user)
                .getResultList();
        for (TeamMember member : memberships) {
            update("""
                    update Todo t set t.assignee = :owner
                    where t.assignee = :user and t.isCompleted = false
                      and t.category in (select c from Category c where c.team = :team)
                    """, Map.of("owner", member.getTeam().getOwner(), "user", user, "team", member.getTeam()));
        }
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

    // ---------- 삭제된 팀 ----------

    public int purgeTeams(LocalDateTime threshold) {
        List<Long> teamIds = transactionTemplate.execute(s -> em.createQuery(
                        "select t.id from Team t where t.deletedAt is not null and t.deletedAt < :threshold", Long.class)
                .setParameter("threshold", threshold)
                .getResultList());
        return runEach(teamIds, "삭제된 팀", this::purgeTeam);
    }

    // 팀 행 삭제 시 채팅방(메시지)·카테고리(투두·게시글)·역할·구성원·초대는 DB가 연쇄 삭제
    private void purgeTeam(Long teamId) {
        List<String> files = strings(
                "select pi.imageUrl from PostImage pi where pi.post.todo.category.team.id = :teamId",
                Map.of("teamId", teamId));
        update("delete from Team t where t.id = :teamId", Map.of("teamId", teamId));
        s3FileManager.deleteAfterCommit(files);
    }

    // ---------- 삭제된 투두 ----------

    // 게시글이 연결된 투두와, 하위 투두가 남아 있는 투두는 제외 (하위부터 여러 차례에 걸쳐 삭제)
    // 원본 투두가 삭제되면 반복 규칙은 연쇄 삭제되고 회차 투두의 routine_id는 null 처리
    public int purgeTodos(LocalDateTime threshold) {
        int total = 0;
        for (int round = 0; round < MAX_TODO_ROUNDS; round++) {
            Integer deleted = transactionTemplate.execute(s -> {
                List<Long> todoIds = em.createQuery("""
                                select t.id from Todo t
                                where t.deletedAt is not null and t.deletedAt < :threshold
                                  and not exists (select p.id from Post p where p.todo = t)
                                  and not exists (select c.id from Todo c where c.parentTodo = t)
                                """, Long.class)
                        .setParameter("threshold", threshold)
                        .setMaxResults(TODO_BATCH_SIZE)
                        .getResultList();
                if (!todoIds.isEmpty()) {
                    update("delete from Todo t where t.id in :ids", Map.of("ids", todoIds));
                }
                return todoIds.size();
            });
            if (deleted == null || deleted == 0) {
                break;
            }
            total += deleted;
        }
        return total;
    }

    // ---------- 공통 ----------

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

    private int runEach(Collection<Long> ids, String label, Consumer<Long> action) {
        int done = 0;
        for (Long id : ids == null ? List.<Long>of() : ids) {
            try {
                transactionTemplate.executeWithoutResult(s -> action.accept(id));
                done++;
            } catch (RuntimeException e) {
                log.error("{} 영구 삭제 실패 : id={}", label, id, e);
            }
        }
        return done;
    }
}
