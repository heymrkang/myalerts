package com.mrkang.myalerts.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 메인 라우팅 컨트롤러 */
@Controller
public class MainController {

    // 1. 메인 페이지 요청을 index 템플릿으로 연결한다.
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
