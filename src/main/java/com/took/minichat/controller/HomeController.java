package com.took.minichat.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String indexRedirect() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 로그인한 사용자라면 chat 페이지로
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/chat/rooms";
        }

        // 로그인 안 되어 있으면 index.html (정적) 표시
        return "redirect:/login";
    }
}
