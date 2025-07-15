package com.took.minichat.controller;

import com.took.minichat.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String nickname,
                           Model model) {

        if (memberService.existsByUsername(username)) {
            model.addAttribute("error", "이미 사용 중인 아이디입니다.");
            return "register";
        }

        if (memberService.existsByNickname(nickname)) {
            model.addAttribute("error", "이미 사용 중인 닉네임입니다.");
            return "register";
        }

        memberService.registerNewMember(username, password, nickname);
        return "redirect:/login";
    }

    @GetMapping("/check/username")
    @ResponseBody
    public Map<String, Boolean> checkUsername(@RequestParam String username) {
        boolean available = !memberService.existsByUsername(username);
        return Map.of("available", available);
    }

    @GetMapping("/check/nickname")
    @ResponseBody
    public Map<String, Boolean> checkNickname(@RequestParam String nickname) {
        boolean available = !memberService.existsByNickname(nickname);
        return Map.of("available", available);
    }

}
