package com.aitechskill.book.ai.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 文字对话请求。
 *
 * @param message 用户文字消息
 */
public record AiChatRequest(
        @NotBlank(message = "对话内容不能为空")
        @Size(max = 20000, message = "对话内容不能超过 20000 字")
        String message) {
}
