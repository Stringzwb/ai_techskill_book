package com.aitechskill.book.english.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 注册技术英语截图导入配置。
 */
@Configuration
@EnableConfigurationProperties(TechEnglishImportProperties.class)
public class TechEnglishImportConfig {
}
