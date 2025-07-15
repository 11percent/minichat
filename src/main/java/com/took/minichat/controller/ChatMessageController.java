package com.took.minichat.controller;

import com.took.minichat.dto.ChatMessageDto;
import com.took.minichat.security.CustomUserDetails;
import com.took.minichat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chat/message")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 일반 HTTP Form 방식으로 메시지 전송
     */
    @PostMapping("/send")
    public String sendMessage(@RequestParam Long roomId,
                              @RequestParam String content,
                              @AuthenticationPrincipal CustomUserDetails user) {
        chatMessageService.sendMessage(roomId, user.getMember().getId(), content);
        return "redirect:/chat/room/" + roomId;
    }

    /**
     * AJAX 방식 메시지 전송 (RESTful)
     */
    @ResponseBody
    @PostMapping(value = "/send/ajax", produces = "application/json")
    public ChatMessageDto sendMessageAjax(@RequestParam Long roomId,
                                          @RequestParam String content,
                                          @RequestParam(required = false) String imageUrl,
                                          @AuthenticationPrincipal CustomUserDetails user) {
        return chatMessageService.sendAndReturnDto(
                roomId,
                user.getMember().getId(),
                content,
                imageUrl,
                "MESSAGE" // ✅ 텍스트 메시지는 기본적으로 MESSAGE 타입으로 설정
        );
    }

    /**
     * WebSocket 방식 메시지 전송
     */
    @MessageMapping("/chat/{roomId}")
    public void sendViaWebSocket(@DestinationVariable Long roomId,
                                 @Payload ChatMessageDto message) {

        // ✅ [1] 읽음 처리 분기
        if ("READ".equalsIgnoreCase(message.getType())) {
            chatMessageService.markMessagesAsRead(roomId, message.getSenderId(), message.getLastReadMessageId());
            return;
        }

        // ✅ [2] 일반 메시지 처리
        ChatMessageDto saved = chatMessageService.saveAndBroadcastMessage(roomId, message);

        // ✅ [3] 메시지 전송
        messagingTemplate.convertAndSend("/topic/room/" + roomId, saved);

        // ✅ [4] 전체 목록 갱신
        messagingTemplate.convertAndSend("/topic/refresh/rooms", "refresh");
    }
}
