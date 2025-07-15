package com.took.minichat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ChatRoom room;

    @ManyToOne(optional = false)
    private Member sender;

    private String content;

    private String imageUrl;

    private LocalDateTime createdAt;

    private boolean isRead;

    // JPA 기본 생성자 (반드시 필요)
    protected ChatMessage() {}

    // 생성자 통한 필드 초기화 (setter 없이 사용)
    public ChatMessage(ChatRoom room, Member sender, String content) {
        this.room = room;
        this.sender = sender;
        this.content = content;
        this.imageUrl = imageUrl;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}