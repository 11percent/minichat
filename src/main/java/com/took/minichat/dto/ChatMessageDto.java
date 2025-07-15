package com.took.minichat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long roomId;
    private String senderNickname;
    private Long senderId;
    private String content;
    private String imageUrl;
    private String createdAt;
    private int unreadCount;
    private String type;
    private Long lastReadMessageId;
}