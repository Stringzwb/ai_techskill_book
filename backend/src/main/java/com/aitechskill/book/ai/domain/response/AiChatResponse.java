package com.aitechskill.book.ai.domain.response;

/**
 * 大模型对话响应。
 *
 * @param text 模型回答
 * @param model 模型名称
 * @param inputTokens 输入 Token 数
 * @param outputTokens 输出 Token 数
 * @param totalTokens 总 Token 数
 */
public record AiChatResponse(
        String text,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens) {
}
