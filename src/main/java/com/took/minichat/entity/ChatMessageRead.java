package com.took.minichat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "ChatMessageRead", uniqueConstraints = {
        @UniqueConstraint(name = "uq_msg_reader", columnNames = {"message_id", "reader_id"})
})
public class ChatMessageRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reader_id", nullable = false)
    private Member reader;

    @Column(nullable = false)
    private LocalDateTime readAt;

    public ChatMessageRead(ChatMessage message, Member reader) {
        this.message = message;
        this.reader = reader;
        this.readAt = LocalDateTime.now();
    }
}
