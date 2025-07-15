package com.took.minichat.repository;

import com.took.minichat.entity.ChatRoom;
import com.took.minichat.entity.ChatRoomMember;
import com.took.minichat.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    boolean existsByRoomAndMember(ChatRoom room, Member member);

    void deleteByRoomAndMember(ChatRoom room, Member member);

    int countByRoom(ChatRoom room);

    @Query("SELECT crm.member FROM ChatRoomMember crm WHERE crm.room.id = :roomId")
    List<Member> findMembersByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT crm.room FROM ChatRoomMember crm WHERE crm.member.id = :memberId")
    List<ChatRoom> findRoomsByMemberId(@Param("memberId") Long memberId);
}