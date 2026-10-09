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

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("select m from Message m join fetch m.chatroom join fetch m.sender where m.id = :id")
    Optional<Message> findWithAllById(@Param("id") Long id);

    @Query("select m from Message m join fetch m.sender where m.sender = :sender and m.clientMessageId = :clientMessageId")
    Optional<Message> findBySenderAndClientMessageId(@Param("sender") User sender, @Param("clientMessageId") String clientMessageId);

    // 참여 시각 이후 메시지 중 beforeId보다 오래된 것 (최신순), 내 화면에서 삭제한 메시지 제외
    @Query("""
            select m from Message m
            join fetch m.sender
            where m.chatroom = :chatroom
              and m.createdAt >= :joinedAt
              and m.id < :beforeId
              and not exists (select h.id from MessageHide h where h.message = m and h.user = :viewer)
            order by m.id desc
            """)
    List<Message> findVisibleMessages(
            @Param("chatroom") Chatroom chatroom,
            @Param("viewer") User viewer,
            @Param("joinedAt") LocalDateTime joinedAt,
            @Param("beforeId") Long beforeId,
            Pageable pageable
    );

    // 채팅방 목록 미리보기 : 내가 참여한 각 채팅방의 참여 이후 마지막 메시지
    @Query("""
            select m from Message m
            join fetch m.sender
            where m.id in (
                select max(m2.id) from Message m2, ChatroomMember cm
                where cm.user = :user
                  and m2.chatroom = cm.chatroom
                  and m2.createdAt >= cm.createdAt
                group by m2.chatroom.id
            )
            """)
    List<Message> findLastMessages(@Param("user") User user);
}
