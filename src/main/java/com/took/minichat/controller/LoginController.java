package com.took.minichat.controller;

import com.took.minichat.dto.ChatMessageDto;
import com.took.minichat.security.CustomUserDetails;
import com.took.minichat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final ChatMessageService chatMessageService;

    // 로그인 페이지 진입
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", true);
        }
        return "login";
    }

//    // 채팅방 상세 진입
//    @GetMapping("/chat/room/{roomId}")
//    public String enterRoom(@PathVariable Long roomId,
//                            @AuthenticationPrincipal CustomUserDetails user,
//                            Model model) {
//
//        Long myId = user.getMember().getId();
//
//        List<ChatMessageDto> messageList = chatMessageService.getMessagesForRoom(roomId);
//
//        model.addAttribute("messages", messageList);
//        model.addAttribute("roomId", roomId);
//        model.addAttribute("myId", myId);
//
//        return "chat/room"; // resources/templates/chat/room.html
//    }
}