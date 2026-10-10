package com.backend.orbitflow.domain.chat.repository;

import com.backend.orbitflow.domain.chat.entity.Chatroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {

    @Query("select c from Chatroom c left join fetch c.team where c.uuid = :uuid")
    Optional<Chatroom> findByUuid(@Param("uuid") String uuid);

    Optional<Chatroom> findByDirectKey(String directKey);

    @Query("select c from Chatroom c left join fetch c.team where c.id in :ids")
    List<Chatroom> findAllWithTeamByIdIn(@Param("ids") Collection<Long> ids);
}
