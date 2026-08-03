package com.aitechskill.book.home;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class KnowledgeHomeController {

    private final KnowledgeHomeService service;

    public KnowledgeHomeController(KnowledgeHomeService service) {
        this.service = service;
    }

    @GetMapping("/home")
    public HomeResponse home() {
        return service.getHome();
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ok", "技术岗AI知识库服务正常");
    }

    public record PingResponse(String status, String message) {
    }
}
