package com.took.minichat.repository;

import com.took.minichat.entity.ChatMessage;
import com.took.minichat.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId);
    Optional<ChatMessage> findFirstByRoomIdOrderByCreatedAtDesc(Long roomId);

    @Query("SELECT m FROM ChatMessage m WHERE m.room.id = :roomId ORDER BY m.createdAt DESC")
    List<ChatMessage> findLatestMessage(@Param("roomId") Long roomId, Pageable pageable);

    void deleteAllByRoom(ChatRoom room);

    @Query("SELECT MAX(m.id) FROM ChatMessage m WHERE m.room.id = :roomId")
    Long findLastMessageIdByRoomId(@Param("roomId") Long roomId);

    // ChatMessageRepository.java
    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.sender WHERE m.room.id = :roomId ORDER BY m.createdAt ASC")
    List<ChatMessage> findWithSenderByRoomIdOrderByCreatedAtAsc(@Param("roomId") Long roomId);


}
