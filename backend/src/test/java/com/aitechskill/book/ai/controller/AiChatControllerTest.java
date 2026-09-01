package com.aitechskill.book.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aitechskill.book.ai.domain.response.AiChatResponse;
import com.aitechskill.book.ai.service.AiChatService;
import com.aitechskill.book.auth.config.AuthInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AI 文字和图片接口测试。
 */
@WebMvcTest(AiChatController.class)
@TestPropertySource(properties = "app.ai.enabled=true")
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiChatService chatService;

    @MockBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void allowAuthenticatedRequest() throws Exception {
        given(authInterceptor.preHandle(any(), any(), any())).willReturn(true);
    }

    @Test
    void returnsTextChatResponse() throws Exception {
        given(chatService.chat("hello"))
                .willReturn(new AiChatResponse("world", "gpt-5.5", 1, 1, 2));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("world"))
                .andExpect(jsonPath("$.model").value("gpt-5.5"));
    }

    @Test
    void returnsVisionChatResponse() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "sample.png", "image/png", new byte[] {1, 2, 3});
        given(chatService.vision("describe", image))
                .willReturn(new AiChatResponse("an image", "gpt-5.5", 10, 2, 12));

        mockMvc.perform(multipart("/api/ai/vision")
                        .file(image)
                        .param("message", "describe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("an image"))
                .andExpect(jsonPath("$.totalTokens").value(12));
    }

    @Test
    void rejectsBlankTextMessage() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
