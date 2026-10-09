package com.backend.orbitflow.domain.notification.repository;

import com.backend.orbitflow.domain.notification.entity.Notification;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 만료되지 않은(threshold 이후 생성) 알림, unreadOnly면 안 읽은 알림만
    @Query(value = """
            select n from Notification n
            left join fetch n.actor
            where n.user = :user
              and n.createdAt > :threshold
              and (:unreadOnly = false or n.isRead = false)
            order by n.createdAt desc, n.id desc
            """,
            countQuery = """
            select count(n) from Notification n
            where n.user = :user
              and n.createdAt > :threshold
              and (:unreadOnly = false or n.isRead = false)
            """)
    Page<Notification> findAllByUser(
            @Param("user") User user,
            @Param("threshold") LocalDateTime threshold,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable
    );

    @Query("""
            select count(n) from Notification n
            where n.user = :user
              and n.createdAt > :threshold
              and n.isRead = false
            """)
    long countUnread(@Param("user") User user, @Param("threshold") LocalDateTime threshold);

    Optional<Notification> findByIdAndUser(Long id, User user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.user = :user and n.isRead = false")
    int markAllRead(@Param("user") User user);

    // 만료 알림 정리
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Notification n where n.createdAt <= :threshold")
    int deleteAllExpired(@Param("threshold") LocalDateTime threshold);
}
