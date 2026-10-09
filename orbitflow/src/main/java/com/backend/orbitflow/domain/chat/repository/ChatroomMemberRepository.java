package com.backend.orbitflow.domain.chat.repository;

import com.backend.orbitflow.domain.chat.dto.ChatroomCount;
import com.backend.orbitflow.domain.chat.entity.Chatroom;
import com.backend.orbitflow.domain.chat.entity.ChatroomMember;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatroomMemberRepository extends JpaRepository<ChatroomMember, Long> {

    Optional<ChatroomMember> findByChatroomAndUser(Chatroom chatroom, User user);

    // STOMP 구독 권한 확인용
    @Query("""
            select count(cm) > 0 from ChatroomMember cm
            where cm.chatroom.uuid = :chatroomUuid
              and cm.user.uuid = :userUuid
            """)
    boolean existsByChatroomUuidAndUserUuid(@Param("chatroomUuid") String chatroomUuid, @Param("userUuid") String userUuid);

    @Query("select cm from ChatroomMember cm join fetch cm.user where cm.chatroom = :chatroom order by cm.createdAt asc")
    List<ChatroomMember> findAllWithUserByChatroom(@Param("chatroom") Chatroom chatroom);

    @Query("""
            select cm from ChatroomMember cm
            join fetch cm.chatroom c
            left join fetch c.team
            where cm.user = :user
            """)
    List<ChatroomMember> findAllWithChatroomByUser(@Param("user") User user);

    @Query("select cm from ChatroomMember cm join fetch cm.user where cm.chatroom in :chatrooms")
    List<ChatroomMember> findAllWithUserByChatroomIn(@Param("chatrooms") List<Chatroom> chatrooms);

    // 안 읽은 메시지 수 : 참여 이후, 마지막 읽음 이후, 다른 사람이 보낸, 전송 취소되지 않은 메시지
    @Query("""
            select new com.backend.orbitflow.domain.chat.dto.ChatroomCount(cm.chatroom.id, count(m))
            from ChatroomMember cm, Message m
            where cm.user = :user
              and m.chatroom = cm.chatroom
              and m.sender <> :user
              and m.unsentAt is null
              and m.createdAt >= cm.createdAt
              and (cm.lastReadAt is null or m.createdAt > cm.lastReadAt)
            group by cm.chatroom.id
            """)
    List<ChatroomCount> countUnread(@Param("user") User user);

    // 팀 탈퇴·추방 시 해당 팀 채팅방에서 제외
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ChatroomMember cm where cm.user = :user and cm.chatroom in (select c from Chatroom c where c.team = :team)")
    void deleteAllByTeamAndUser(@Param("team") Team team, @Param("user") User user);

    // 팀 삭제 시 팀 채팅방 참여자 전원 제외
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ChatroomMember cm where cm.chatroom in (select c from Chatroom c where c.team = :team)")
    void deleteAllByTeam(@Param("team") Team team);
}
