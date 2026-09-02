package com.aitechskill.book.ai.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * OpenAI 兼容大模型配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai")
public class AiModelProperties {

    /** 是否启用 AI 调用接口。 */
    private boolean enabled;

    /** OpenAI 兼容 API 根地址。 */
    private String baseUrl = "https://www.bb-api.com/v1";

    /** 文本和视觉共用的模型名称。 */
    private String model = "gpt-5.5";

    /** 主用 API 密钥。 */
    private String primaryApiKey;

    /** 备用 API 密钥。 */
    private String secondaryApiKey;

    /** 单次模型请求超时时间。 */
    private Duration timeout = Duration.ofSeconds(180);

    /** 每个 Key 的 SDK 内部重试次数。 */
    private int maxRetries;

    /** 同一进程允许同时进行的视觉模型请求数。 */
    private int maxConcurrentVisionRequests = 1;

    /** 单次回答最大 Token 数。 */
    private int maxOutputTokens = 1000;

    /** 视觉接口允许的最大图片体积。 */
    private DataSize imageMaxSize = DataSize.ofMegabytes(10);
}
