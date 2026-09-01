package com.aitechskill.book.ai.controller;

import com.aitechskill.book.ai.domain.request.AiChatRequest;
import com.aitechskill.book.ai.domain.response.AiChatResponse;
import com.aitechskill.book.ai.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 提供文字对话和图片加文字对话的备用 AI 接口。
 */
@RestController
@RequestMapping("/api/ai")
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class AiChatController {

    private final AiChatService chatService;

    public AiChatController(AiChatService chatService) {
        this.chatService = chatService;
    }

    /** 发送文字消息给大模型。 */
    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return chatService.chat(request.message());
    }

    /** 上传图片并同时发送文字消息给大模型。 */
    @PostMapping(value = "/vision", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AiChatResponse vision(
            @RequestParam String message,
            @RequestPart("image") MultipartFile image) {
        return chatService.vision(message, image);
    }
}
