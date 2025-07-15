package com.took.minichat.repository;

import com.took.minichat.entity.ChatRoom;
import com.took.minichat.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    @Query("SELECT r FROM ChatRoom r WHERE r.user1 = :user1 AND r.user2 = :user2")
    Optional<ChatRoom> findByUserPair(@Param("user1") Member user1, @Param("user2") Member user2);

    @Query("""
    SELECT r FROM ChatRoom r
    JOIN ChatRoomMember crm ON crm.room = r
    WHERE crm.member.id IN (:user1Id, :user2Id)
    GROUP BY r
    HAVING COUNT(DISTINCT crm.member.id) = 2
       AND COUNT(crm) = 2
""")
    List<ChatRoom> findDirectChatRoom(Long user1Id, Long user2Id);


}