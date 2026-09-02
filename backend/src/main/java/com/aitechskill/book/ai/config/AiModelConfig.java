package com.aitechskill.book.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 创建 OpenAI 兼容的 LangChain4j 主备模型客户端。
 */
@Configuration
@EnableConfigurationProperties(AiModelProperties.class)
public class AiModelConfig {

    /**
     * 按主 Key、备用 Key 顺序创建模型客户端。
     *
     * @param properties AI 配置
     * @return 主备模型客户端
     */
    @Bean
    @ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
    AiModelClients aiModelClients(AiModelProperties properties) {
        requireConfigured(properties.getBaseUrl(), "AI_BASE_URL");
        requireConfigured(properties.getModel(), "AI_MODEL");
        if (properties.getMaxOutputTokens() <= 0) {
            throw new IllegalStateException("AI_MAX_OUTPUT_TOKENS 必须大于 0");
        }
        if (properties.getMaxRetries() < 0) {
            throw new IllegalStateException("AI_MAX_RETRIES 不能小于 0");
        }
        if (properties.getMaxConcurrentVisionRequests() < 1) {
            throw new IllegalStateException("AI_MAX_CONCURRENT_VISION_REQUESTS 必须大于 0");
        }

        Set<String> apiKeys = new LinkedHashSet<>();
        addKey(apiKeys, properties.getPrimaryApiKey());
        addKey(apiKeys, properties.getSecondaryApiKey());
        if (apiKeys.isEmpty()) {
            throw new IllegalStateException("AI_ENABLED=true 时至少配置一个 AI API Key");
        }

        List<ChatLanguageModel> models = apiKeys.stream()
                .map(apiKey -> createModel(properties, apiKey))
                .toList();
        return new AiModelClients(models);
    }

    /** 创建单个不记录请求和响应正文的模型客户端。 */
    private ChatLanguageModel createModel(AiModelProperties properties, String apiKey) {
        return OpenAiChatModel.builder()
                .baseUrl(properties.getBaseUrl().trim())
                .apiKey(apiKey)
                .modelName(properties.getModel().trim())
                .maxCompletionTokens(properties.getMaxOutputTokens())
                .timeout(properties.getTimeout())
                .maxRetries(properties.getMaxRetries())
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    /** 添加去除首尾空白后的 API Key。 */
    private void addKey(Set<String> apiKeys, String apiKey) {
        if (StringUtils.hasText(apiKey)) {
            apiKeys.add(apiKey.trim());
        }
    }

    /** 校验启用 AI 时必需的普通配置。 */
    private void requireConfigured(String value, String environmentName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("AI_ENABLED=true 时必须配置 " + environmentName);
        }
    }
}
