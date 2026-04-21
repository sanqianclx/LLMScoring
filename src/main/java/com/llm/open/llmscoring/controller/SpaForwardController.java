package com.llm.open.llmscoring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/login",
            "/register",
            "/forgot-password",
            "/teacher/dashboard",
            "/teacher/courses",
            "/teacher/papers",
            "/teacher/papers/new",
            "/teacher/papers/{paperId}/edit",
            "/teacher/share/{paperId}",
            "/teacher/review",
            "/teacher/profile",
            "/student",
            "/student/exam/{shareCode}",
            "/student/result/{shareCode}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}