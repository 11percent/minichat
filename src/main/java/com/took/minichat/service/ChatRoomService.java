package com.took.minichat.service;

import com.took.minichat.dto.ChatMessageDto;
import com.took.minichat.dto.ChatRoomDto;
import com.took.minichat.entity.*;
import com.took.minichat.repository.*;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageReadRepository chatMessageReadRepository;
    private final S3Uploader s3Uploader;

    public Long createOrGetRoom(Long userId, Long opponentId) {
        return chatRoomRepository.findByUser1IdAndUser2Id(userId, opponentId)
                .orElseGet(() -> {
                    Member user1 = memberRepository.findById(userId).orElseThrow();
                    Member user2 = memberRepository.findById(opponentId).orElseThrow();
                    ChatRoom room = ChatRoom.create(user1, user2); // 이 메서드 안에서 모든 세팅 처리
                    return chatRoomRepository.save(room);
                }).getId();
    }

    public List<ChatRoomDto> getRoomsForUser(Long memberId) {
        List<ChatRoom> rooms = chatRoomMemberRepository.findRoomsByMemberId(memberId);
        Member currentUser = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return rooms.stream()
                .map(room -> {
                    List<Member> members = chatRoomMemberRepository.findMembersByRoomId(room.getId());
                    List<String> nicknames = members.stream()
                            .map(Member::getNickname)
                            .collect(Collectors.toList());

                    Pageable pageable = PageRequest.of(0, 1);
                    var lastMessageOpt = chatMessageRepository.findLatestMessage(room.getId(), pageable)
                            .stream().findFirst();

                    int unreadCount = (int) chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(room.getId()).stream()
                            .filter(m -> !m.getSender().getId().equals(memberId))
                            .filter(m -> !chatMessageReadRepository.existsByMessageAndReader(m, currentUser))
                            .count();

                    return ChatRoomDto.builder()
                            .id(room.getId())
                            .roomName(String.join(", ", nicknames))
                            .memberNicknames(nicknames)
                            .lastMessageTime(lastMessageOpt.map(ChatMessage::getCreatedAt).orElse(room.getCreatedAt()))
                            .lastMessageContent(lastMessageOpt.map(ChatMessage::getContent).orElse(""))
                            .lastMessageType(lastMessageOpt.map(msg -> {
                                if (msg.getContent() != null && !msg.getContent().isBlank()) {
                                    return "MESSAGE";
                                } else if (msg.getImageUrl() != null && !msg.getImageUrl().isBlank()) {
                                    return "IMAGE";
                                } else {
                                    return "UNKNOWN";
                                }
                            }).orElse(null))
                            .unreadCount(unreadCount)
                            .build();
                })
                .toList();
    }



    public Member findMemberByNickname(String nickname) {
        return memberRepository.findByNickname(nickname).orElse(null);
    }

    public Long createOrGetRoomByNickname(Long myId, String opponentNickname) {
        Member me = memberRepository.findById(myId)
                .orElseThrow(() -> new IllegalArgumentException("본인 정보 없음"));

        Member opponent = memberRepository.findByNickname(opponentNickname)
                .orElseThrow(() -> new IllegalArgumentException("상대방이 존재하지 않습니다."));

        // Step 1: 내가 속한 모든 채팅방 가져오기
        List<ChatRoom> myRooms = chatRoomMemberRepository.findRoomsByMemberId(myId);

        // Step 2: 각 채팅방마다 참여자 수가 2명이면서 나와 상대방만 있는 방이 있는지 확인
        for (ChatRoom room : myRooms) {
            List<Member> members = chatRoomMemberRepository.findMembersByRoomId(room.getId());

            if (members.size() == 2 &&
                    members.stream().anyMatch(m -> m.getId().equals(opponent.getId()))) {
                return room.getId(); // 👉 기존 1:1 채팅방이므로 바로 반환
            }
        }

        // Step 3: 없다면 새 채팅방 생성
        ChatRoom newRoom = ChatRoom.create(me, opponent); // 이 안에서 user1, user2 설정
        chatRoomRepository.save(newRoom);

        chatRoomMemberRepository.save(new ChatRoomMember(newRoom, me));
        chatRoomMemberRepository.save(new ChatRoomMember(newRoom, opponent));

        return newRoom.getId();
    }



    //    여기부터 다중 채팅방 수정
    public void inviteMemberByNickname(Long roomId, String nickname) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("해당 닉네임의 회원이 존재하지 않습니다."));

        boolean alreadyJoined = chatRoomMemberRepository.existsByRoomAndMember(room, member);
        if (alreadyJoined) {
            throw new IllegalStateException("이미 참여 중인 사용자입니다.");
        }

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .room(room)
                .member(member)
                .build();

        chatRoomMemberRepository.save(chatRoomMember);
    }

    // 유저 나가기
    @Transactional
    public void leaveRoom(Long roomId, Long memberId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 1. ChatRoomMember에서 나 자신 삭제
        chatRoomMemberRepository.deleteByRoomAndMember(room, member);

        // 2. 남은 참여자 수 확인
        int remainingMembers = chatRoomMemberRepository.countByRoom(room);

        if (remainingMembers == 0) {
            // ✅ 2-1. 이 방의 메시지를 모두 가져온다
            List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);

            // ✅ 2-2. 각 메시지에 연결된 읽음 기록 먼저 삭제
            for (ChatMessage message : messages) {
                chatMessageReadRepository.deleteAllByMessages(List.of(message));

                if (message.getImageUrl() != null) {
                    s3Uploader.delete(message.getImageUrl()); // ✅ 이미지가 있으면 S3에서 삭제
                }

                chatMessageRepository.delete(message);
            }

            // ✅ 2-3. 메시지 삭제
            chatMessageRepository.deleteAll(messages);

            // ✅ 2-4. 채팅방 삭제
            chatRoomRepository.delete(room);
        }
    }


    // 채팅방 단건 조회
    public ChatRoom getRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
    }

    // 채팅방의 모든 참여자 조회
    public List<Member> getMembersInRoom(Long roomId) {
        return chatRoomMemberRepository.findMembersByRoomId(roomId);
    }

    public List<ChatRoom> getChatRoomsForMember(Long memberId) {
        return chatRoomMemberRepository.findRoomsByMemberId(memberId);
    }

}
