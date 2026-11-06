package com.mrkang.myalerts.controller;

import com.mrkang.myalerts.dao.User;
import com.mrkang.myalerts.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/** 메인 라우팅 컨트롤러 */
@Controller
@RequiredArgsConstructor
public class MainController {

    private final AuthService authService;

    // 1. 메인 화면 요청 처리 (사용자 인증 상태 확인 후 모델 전달)
    @GetMapping("/")
    public String index(HttpServletRequest request, HttpServletResponse response, Model model) {
        User user = authService.resolveUser(request, response);
        model.addAttribute("user", user);
        return "main/index";
    }

    // 2. 로그인 처리 API
    @PostMapping("/login")
    @ResponseBody
    public Map<String, Object> login(
            @RequestParam("userId") String userId,
            @RequestParam("userPw") String userPw,
            HttpServletResponse response) {
        return authService.login(userId, userPw, response);
    }

    // 3. 회원가입 처리 API
    @PostMapping("/register")
    @ResponseBody
    public Map<String, Object> register(
            @RequestParam("userId") String userId,
            @RequestParam("userPw") String userPw) {
        return authService.register(userId, userPw);
    }

    // 4. 로그아웃 처리 및 메인 리다이렉트
    @PostMapping("/logout")
    public String logout(
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletResponse response) {
        authService.logout(userId, response);
        return "redirect:/";
    }
}
