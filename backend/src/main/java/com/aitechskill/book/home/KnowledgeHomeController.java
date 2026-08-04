package com.aitechskill.book.home;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开首页接口。
 */
@RestController
@RequestMapping("/api")
public class KnowledgeHomeController {

    private final KnowledgeHomeService service;

    public KnowledgeHomeController(KnowledgeHomeService service) {
        this.service = service;
    }

    /** 查询首页内容。 */
    @GetMapping("/home")
    public HomeResponse home() {
        return service.getHome();
    }

    /** 查询服务连通状态。 */
    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ok", "技术岗AI知识库服务正常");
    }

    /** 服务连通状态响应。 */
    public record PingResponse(String status, String message) {
    }
}
