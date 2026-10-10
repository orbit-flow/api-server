package com.backend.orbitflow.domain.chat.repository;

import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.entity.Message;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import java.util.Collection;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("select m from Message m join fetch m.chatroom join fetch m.sender where m.id = :id")
    Optional<Message> findWithAllById(@Param("id") Long id);

    @Query("select m from Message m join fetch m.sender where m.chatroom = :chatroom and m.sender = :sender and m.clientMessageId = :clientMessageId")
    Optional<Message> findByChatroomAndSenderAndClientMessageId(@Param("chatroom") Chatroom chatroom, @Param("sender") User sender, @Param("clientMessageId") String clientMessageId);

    // 참여 시각 이후 메시지 중 beforeId보다 오래된 것 (최신순), 내 화면에서 삭제한 메시지 제외
    @Query(value = """
            select m from Message m
            join fetch m.sender
            where m.chatroom = :chatroom
              and m.createdAt >= :joinedAt
              and m.id < :beforeId
              and not exists (select h.id from MessageHide h where h.message = m and h.user = :viewer)
            order by m.id desc
            """,
            countQuery = """
            select count(m) from Message m
            where m.chatroom = :chatroom
              and m.createdAt >= :joinedAt
              and m.id < :beforeId
              and not exists (select h.id from MessageHide h where h.message = m and h.user = :viewer)
            """)
    Page<Message> findVisibleMessages(
            @Param("chatroom") Chatroom chatroom,
            @Param("viewer") User viewer,
            @Param("joinedAt") LocalDateTime joinedAt,
            @Param("beforeId") Long beforeId,
            Pageable pageable
    );

    // 채팅방 목록 미리보기 : 목록 페이지에서 구한 마지막 메시지 id로 조회
    @Query("select m from Message m join fetch m.sender where m.id in :ids")
    List<Message> findAllWithSenderByIdIn(@Param("ids") Collection<Long> ids);
}
