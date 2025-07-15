package com.took.minichat.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Member user1;

    @ManyToOne
    private Member user2;

    private LocalDateTime createdAt;

    protected ChatRoom() {}


    // 정적 팩토리 메서드 사용 권장
    public static ChatRoom create(Member user1, Member user2) {
        ChatRoom room = new ChatRoom();
        room.user1 = user1;
        room.user2 = user2;
        room.createdAt = LocalDateTime.now();
        return room;
    }

    // Getter만 공개 (Setter는 제거)
    public Member getUser1() { return user1; }
    public Member getUser2() { return user2; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
