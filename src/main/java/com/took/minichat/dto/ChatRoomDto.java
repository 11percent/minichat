package com.took.minichat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ChatRoomDto {
    private Long id;
    private String roomName; // ✅ 채팅방 이름 또는 구성원 요약
    private List<String> memberNicknames; // ✅ 참여자 전체 닉네임 목록
    private LocalDateTime lastMessageTime;
    private String lastMessageContent;
    private String lastMessageType;
    private int unreadCount;
}