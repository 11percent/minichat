package com.took.minichat.repository;

import com.took.minichat.entity.ChatMessage;
import com.took.minichat.entity.ChatMessageRead;
import com.took.minichat.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatMessageReadRepository extends JpaRepository<ChatMessageRead, Long> {
    boolean existsByMessageAndReader(ChatMessage message, Member reader);
    int countByMessage(ChatMessage message);
    boolean existsByMessageIdAndReaderId(Long messageId, Long readerId);

    @Query("SELECT COUNT(r) FROM ChatMessageRead r WHERE r.message = :message AND r.reader.id != :senderId")
    int countByMessageExcludingSender(@Param("message") ChatMessage message, @Param("senderId") Long senderId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ChatMessageRead r WHERE r.message IN :messages")
    void deleteAllByMessages(@Param("messages") List<ChatMessage> messages);

}
