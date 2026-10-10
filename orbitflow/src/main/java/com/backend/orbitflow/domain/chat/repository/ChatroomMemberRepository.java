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
import com.backend.orbitflow.domain.chat.dto.ChatroomPageRow;
import com.backend.orbitflow.domain.chat.dto.response.ChatroomMemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.Set;

public interface ChatroomMemberRepository extends JpaRepository<ChatroomMember, Long> {

    Optional<ChatroomMember> findByChatroomAndUser(Chatroom chatroom, User user);

    // STOMP 구독 권한 확인용
    @Query("""
            select count(cm) > 0 from ChatroomMember cm
            where cm.chatroom.uuid = :chatroomUuid
              and cm.user.uuid = :userUuid
            """)
    boolean existsByChatroomUuidAndUserUuid(@Param("chatroomUuid") String chatroomUuid, @Param("userUuid") String userUuid);

    // 구독 재확인용 : 주어진 채팅방·사용자 중 실제 참여 관계인 (채팅방 uuid, 사용자 uuid) 쌍
    @Query("""
            select c.uuid as chatroomUuid, u.uuid as userUuid
            from ChatroomMember cm
            join cm.chatroom c
            join cm.user u
            where c.uuid in :chatroomUuids
              and u.uuid in :userUuids
            """)
    List<MemberUuids> findMemberUuidsIn(@Param("chatroomUuids") Collection<String> chatroomUuids,
                                        @Param("userUuids") Collection<String> userUuids);

    interface MemberUuids {

        String getChatroomUuid();
        String getUserUuid();
    }

    @Query("select cm from ChatroomMember cm join fetch cm.user where cm.chatroom = :chatroom order by cm.createdAt asc")
    List<ChatroomMember> findAllWithUserByChatroom(@Param("chatroom") Chatroom chatroom);


    // 내 채팅방 목록 페이지 : 참여 이후 마지막 메시지 최신순 (메시지 없는 방은 뒤로), 내 화면에서 삭제한 메시지는 제외
    @Query(nativeQuery = true, value = """
            select x.chatroom_id as chatroomId, x.last_message_id as lastMessageId
            from (
                select cm.chatroom_id,
                       (select max(m.id) from messages m
                        where m.chatroom_id = cm.chatroom_id and m.created_at >= cm.created_at
                          and not exists (select 1 from message_hides h where h.message_id = m.id and h.user_id = cm.user_id)) as last_message_id
                from chatroom_members cm
                where cm.user_id = :userId
            ) x
            order by coalesce(x.last_message_id, 0) desc, x.chatroom_id desc
            """,
            countQuery = "select count(*) from chatroom_members cm where cm.user_id = :userId")
    Page<ChatroomPageRow> findMyChatroomPage(@Param("userId") Long userId, Pageable pageable);

    // 채팅방별 참여자 수
    @Query("""
            select new com.backend.orbitflow.domain.chat.dto.ChatroomCount(cm.chatroom.id, count(cm))
            from ChatroomMember cm
            where cm.chatroom.id in :chatroomIds
            group by cm.chatroom.id
            """)
    List<ChatroomCount> countMembersIn(@Param("chatroomIds") Collection<Long> chatroomIds);

    long countByChatroom(Chatroom chatroom);

    // 1:1 대화의 상대방 참여 정보 (나 제외)
    @Query("""
            select cm from ChatroomMember cm
            join fetch cm.user
            where cm.chatroom.id in :chatroomIds
              and cm.chatroom.type = com.backend.orbitflow.domain.chat.enums.ChatroomType.DIRECT
              and cm.user <> :me
            """)
    List<ChatroomMember> findDirectCounterparts(@Param("chatroomIds") Collection<Long> chatroomIds, @Param("me") User me);

    // 참여자 목록 페이지 (참여 순, 응답 항목으로 바로 조회)
    @Query(value = """
            select new com.backend.orbitflow.domain.chat.dto.response.ChatroomMemberResponse(u.uuid, u.name, u.profileImage, cm.createdAt)
            from ChatroomMember cm
            join cm.user u
            where cm.chatroom = :chatroom
            order by cm.createdAt asc, cm.id asc
            """,
            countQuery = "select count(cm) from ChatroomMember cm where cm.chatroom = :chatroom")
    Page<ChatroomMemberResponse> findMemberPage(@Param("chatroom") Chatroom chatroom, Pageable pageable);

    // 이미 참여 중인 사용자 id (초대 대상 중)
    @Query("select cm.user.id from ChatroomMember cm where cm.chatroom = :chatroom and cm.user.id in :userIds")
    Set<Long> findMemberUserIdsAmong(@Param("chatroom") Chatroom chatroom, @Param("userIds") Collection<Long> userIds);

    // 채팅방 참여자 id 전체 (초대 시 구성원 간 차단 검증용)
    @Query("select cm.user.id from ChatroomMember cm where cm.chatroom = :chatroom")
    List<Long> findMemberUserIds(@Param("chatroom") Chatroom chatroom);

    // 안 읽은 메시지 수 : 참여 이후, 마지막 읽음 이후, 다른 사람이 보낸, 전송 취소되지 않은, 내 화면에서 삭제하지 않은 메시지
    @Query("""
            select new com.backend.orbitflow.domain.chat.dto.ChatroomCount(cm.chatroom.id, count(m))
            from ChatroomMember cm, Message m
            where cm.user = :user
              and m.chatroom = cm.chatroom
              and m.sender <> :user
              and m.unsentAt is null
              and not exists (select h.id from MessageHide h where h.message = m and h.user = :user)
              and m.createdAt >= cm.createdAt
              and (cm.lastReadAt is null or m.createdAt > cm.lastReadAt)
              and cm.chatroom.id in :chatroomIds
            group by cm.chatroom.id
            """)
    List<ChatroomCount> countUnreadIn(@Param("user") User user, @Param("chatroomIds") Collection<Long> chatroomIds);

    // 팀 탈퇴·추방 시 해당 팀 채팅방에서 제외
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ChatroomMember cm where cm.user = :user and cm.chatroom in (select c from Chatroom c where c.team = :team)")
    void deleteAllByTeamAndUser(@Param("team") Team team, @Param("user") User user);

    // 팀 삭제 시 팀 채팅방 참여자 전원 제외
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ChatroomMember cm where cm.chatroom in (select c from Chatroom c where c.team = :team)")
    void deleteAllByTeam(@Param("team") Team team);
}
