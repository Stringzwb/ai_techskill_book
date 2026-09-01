package com.aitechskill.book.english.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 技术英语 AI 截图导入配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.tech-english.ai-import")
public class TechEnglishImportProperties {

    /** 当前截图语料的固定来源。 */
    private String sourceName = "薄荷阅读";

    /** 单次允许上传的最大截图数。 */
    private int maxImages = 10;

    /** 单条语料允许生成的最大例句数。 */
    private int maxExampleCount = 5;

    /** 识别结果等待用户确认的有效期。 */
    private Duration draftTtl = Duration.ofMinutes(30);
}
