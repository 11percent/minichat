package com.took.minichat.service;

import com.took.minichat.entity.Member;
import com.took.minichat.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean existsByUsername(String username) {
        return memberRepository.findByUsername(username).isPresent();
    }

    public boolean existsByNickname(String nickname) {
        return memberRepository.findByNickname(nickname).isPresent();
    }

    public void registerNewMember(String username, String rawPassword, String nickname) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        Member member = new Member(username, encodedPassword, nickname);
        memberRepository.save(member);
    }
}
