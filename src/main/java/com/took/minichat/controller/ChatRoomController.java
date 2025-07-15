package com.took.minichat.controller;

import com.took.minichat.dto.ChatMessageDto;
import com.took.minichat.dto.ChatRoomDto;
import com.took.minichat.entity.ChatRoom;
import com.took.minichat.entity.ChatRoomMember;
import com.took.minichat.entity.Member;
import com.took.minichat.security.CustomUserDetails;
import com.took.minichat.service.ChatMessageService;
import com.took.minichat.service.ChatRoomService;
import com.took.minichat.service.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @GetMapping("/chat/rooms")
    public String chatRoomList(Model model,
                               @AuthenticationPrincipal CustomUserDetails user,
                               @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        List<ChatRoomDto> rooms = chatRoomService.getRoomsForUser(user.getMember().getId());
        model.addAttribute("rooms", rooms);

        String nickName = user.getMember().getNickname(); // ✅ 닉네임 추가
        model.addAttribute("nickName", nickName);

        log.info("🚨 requestedWith: {}", requestedWith);
        log.info("✅ rooms.size: {}", rooms.size());

        if ("XMLHttpRequest".equals(requestedWith)) {
            return "chat/rooms :: roomTableBody"; // Thymeleaf fragment
        }
        return "chat/rooms";
    }

    @PostMapping("/chat/room/create")
    @ResponseBody
    public ResponseEntity<?> createRoom(@RequestParam String opponentNickname,
                                        @AuthenticationPrincipal CustomUserDetails user) {
        Long myId = user.getMember().getId();
        String myNickname = user.getMember().getNickname();

        // 1. 본인 닉네임으로 생성 시도한 경우
        if (opponentNickname.equals(myNickname)) {
            return ResponseEntity.badRequest().body(Map.of("error", "자기 자신과는 채팅할 수 없습니다."));
        }

        // 2. 존재하는 회원인지 확인
        Member opponent = chatRoomService.findMemberByNickname(opponentNickname);
        if (opponent == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "존재하지 않는 닉네임입니다."));
        }

        // 3. 채팅방 생성 or 기존 채팅방 반환
        Long roomId = chatRoomService.createOrGetRoomByNickname(myId, opponentNickname);
        return ResponseEntity.ok(Map.of("roomUrl", "/chat/room/" + roomId));
    }

    @GetMapping("/chat/room/{roomId}")
    public String viewChatRoom(@PathVariable Long roomId,
                               @AuthenticationPrincipal CustomUserDetails user,
                               Model model) {
        if (user == null) {
            return "redirect:/login"; // 또는 로그인 페이지 경로
        }

        Long myId = user.getMember().getId();

        ChatRoom room = chatRoomService.getRoom(roomId);
        List<Member> members = chatRoomService.getMembersInRoom(roomId);
        List<ChatMessageDto> messages = chatMessageService.getMessagesForRoom(roomId);

        model.addAttribute("room", room);
        model.addAttribute("roomMembers", members);
        model.addAttribute("messages", messages);
        model.addAttribute("roomId", roomId);
        model.addAttribute("myId", myId);

        return "chat/room";
    }

    @PostMapping("/chat/room/{roomId}/invite")
    public String inviteMember(@PathVariable Long roomId,
                               @RequestParam("nickname") String nickname) {
        chatRoomService.inviteMemberByNickname(roomId, nickname);
        return "redirect:/chat/room/" + roomId;
    }

    @PostMapping("/chat/room/{roomId}/leave")
    public String leaveRoom(@PathVariable Long roomId,
                            @AuthenticationPrincipal CustomUserDetails user,
                            Model model) {
        chatRoomService.leaveRoom(roomId, user.getMember().getId());
        return "chat/close-popup"; // ✅ 이 HTML에서 부모창 갱신 + 팝업 닫기
    }
//
//    @RestController
//    @RequiredArgsConstructor
//    public class ChatImageController {
//
//        private final S3Uploader s3Uploader;
//        private final SimpMessagingTemplate messagingTemplate;
//
//        @PostMapping("/api/chat/image")
//        public ResponseEntity<?> uploadImage(
//                @RequestParam MultipartFile file,
//                @RequestParam String roomId,
//                @AuthenticationPrincipal CustomUserDetails user) throws IOException {
//
//            String imageUrl = s3Uploader.upload(file);
//
//            ChatMessageDto message = new ChatMessageDto();
//            message.setSenderId(user.getMember().getId());
//            message.setSenderNickname(user.getMember().getNickname());
//            message.setRoomId(Long.parseLong(roomId));
//            message.setContent(null);
//            message.setImageUrl(imageUrl); // ✅ 이미지 URL 전달
//            message.setType("IMAGE");
//            message.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
//            message.setUnreadCount(0);
//            message.setSenderId(-1L); // ✅ senderId는 프론트에서 본인 여부 판단 시 꼭 필요
//            // msg.id 도 없는 상태이므로 null임
//
//            messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
//
//            return ResponseEntity.ok().body(imageUrl);
//        }
//    }

}