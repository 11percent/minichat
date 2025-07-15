package com.took.minichat.controller;

import com.took.minichat.dto.ChatMessageDto;
import com.took.minichat.security.CustomUserDetails;
import com.took.minichat.service.ChatMessageService;
import com.took.minichat.service.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class ChatImageController {
    private final S3Uploader s3Uploader;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/api/chat/image")
    public ResponseEntity<?> uploadImage(
            @RequestParam MultipartFile file,
            @RequestParam Long roomId,
            @AuthenticationPrincipal CustomUserDetails user) throws IOException {

        // 1. S3 업로드
        String imageUrl = s3Uploader.upload(file);

        // 2. DTO 생성
        ChatMessageDto temp = ChatMessageDto.builder()
                .senderId(user.getMember().getId())
                .imageUrl(imageUrl)
                .type("IMAGE")
                .build();

        // 3. 메시지 저장 및 unreadCount 계산 포함
        ChatMessageDto savedMessage = chatMessageService.saveAndBroadcastMessage(roomId, temp);

        // 4. WebSocket 전송
        messagingTemplate.convertAndSend("/topic/room/" + roomId, savedMessage);

        // 5. 클라이언트 응답
        return ResponseEntity.ok().body(imageUrl);
    }
}
