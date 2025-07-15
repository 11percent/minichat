package com.took.minichat.service;

import com.took.minichat.dto.ChatMessageDto;
import com.took.minichat.entity.ChatMessage;
import com.took.minichat.entity.ChatMessageRead;
import com.took.minichat.entity.ChatRoom;
import com.took.minichat.entity.Member;
import com.took.minichat.repository.*;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageReadRepository chatMessageReadRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public void sendMessage(Long roomId, Long senderId, String content) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
        Member sender = memberRepository.findById(senderId).orElseThrow();

        ChatMessage message = new ChatMessage(room, sender, content);
        chatMessageRepository.save(message);
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    public List<ChatMessageDto> getMessagesForRoom(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
        List<ChatMessage> messages = chatMessageRepository.findWithSenderByRoomIdOrderByCreatedAtAsc(roomId);

        return messages.stream()
                .map(msg -> {
                    int totalParticipants = chatRoomMemberRepository.countByRoom(room);
                    int readCount = chatMessageReadRepository.countByMessage(msg);

                    int unreadCount = totalParticipants - readCount - 1; // ✅ 보낸 사람 제외
                    if (unreadCount < 0) unreadCount = 0; // ✅ 안전하게 보정

                    return ChatMessageDto.builder()
                            .id(msg.getId())
                            .roomId(roomId)
                            .senderNickname(msg.getSender().getNickname())
                            .senderId(msg.getSender().getId())
                            .content(msg.getContent())
                            .imageUrl(msg.getImageUrl())
                            .createdAt(msg.getCreatedAt().format(formatter))
                            .unreadCount(unreadCount)
                            .type(msg.getImageUrl() != null ? "IMAGE" : "MESSAGE")
                            .build();
                })
                .toList();
    }

    public ChatMessageDto sendAndReturnDto(Long roomId, Long senderId, String content, String imageUrl, String type) {
        if ((content == null || content.trim().isEmpty()) && (imageUrl == null || imageUrl.trim().isEmpty())) {
            throw new IllegalArgumentException("내용 또는 이미지를 입력해주세요.");
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 채팅방입니다."));
        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(content)
                .imageUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();

        chatMessageRepository.save(message);

        return ChatMessageDto.builder()
                .id(message.getId())
                .roomId(room.getId())
                .senderNickname(sender.getNickname())
                .senderId(sender.getId())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .createdAt(message.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                .unreadCount(0)
                .type(type)  // ✅ 여기를 하드코딩하지 않고 전달받은 type 사용
                .build();
    }


    public ChatMessageDto saveAndBroadcastMessage(Long roomId, ChatMessageDto messageDto) {
        Member sender = memberRepository.findById(messageDto.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(messageDto.getContent())
                .imageUrl(messageDto.getImageUrl())
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();

        chatMessageRepository.save(message);

        // ✅ 보낸 사람 제외한 미열람자 수 계산
        int totalParticipants = chatRoomMemberRepository.countByRoom(room);
        int unreadCount = totalParticipants - 1;
        if (unreadCount < 0) unreadCount = 0;

        messagingTemplate.convertAndSend("/topic/refresh/rooms", "refresh");

        return ChatMessageDto.builder()
                .id(message.getId())
                .roomId(roomId)
                .senderNickname(sender.getNickname())
                .senderId(sender.getId())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .createdAt(message.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                .unreadCount(unreadCount)
                .type(messageDto.getType())
                .build();
    }

    @Transactional
    public void markMessagesAsRead(Long roomId, Long readerId, Long lastReadMessageId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
        Member reader = memberRepository.findById(readerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        List<ChatMessage> messages = chatMessageRepository.findWithSenderByRoomIdOrderByCreatedAtAsc(roomId);

        for (ChatMessage message : messages) {
            boolean alreadyRead = chatMessageReadRepository.existsByMessageAndReader(message, reader);

            if (!alreadyRead
                    && !Objects.equals(message.getSender().getId(), readerId)
                    && message.getId() <= lastReadMessageId) {

                chatMessageReadRepository.save(new ChatMessageRead(message, reader));

                int totalParticipants = chatRoomMemberRepository.countByRoom(room);
                int readCount = chatMessageReadRepository.countByMessageExcludingSender(message, message.getSender().getId());
                int unreadCount = totalParticipants - 1 - readCount;

                ChatMessageDto updateDto = ChatMessageDto.builder()
                        .id(message.getId())
                        .roomId(roomId)
                        .senderId(message.getSender().getId())
                        .senderNickname(message.getSender().getNickname())
                        .content(message.getContent())
                        .imageUrl(message.getImageUrl())
                        .createdAt(message.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                        .unreadCount(unreadCount)
                        .type("READ")
                        .build();

                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/read", updateDto);
            }
        }

        messagingTemplate.convertAndSend("/topic/refresh/rooms", "refresh");

    }

    @Transactional
    public void markMessagesAsRead(Long roomId, Long readerId) {
        Long lastMessageId = chatMessageRepository.findLastMessageIdByRoomId(roomId);
        if (lastMessageId != null) {
            markMessagesAsRead(roomId, readerId, lastMessageId);
        }
    }
}
