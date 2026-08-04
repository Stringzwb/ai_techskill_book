package com.aitechskill.book.storage.config;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 对象存储客户端配置。
 */
@Configuration
@EnableConfigurationProperties(ObjectStorageProperties.class)
public class ObjectStorageConfig {

    /**
     * 创建 S3 兼容客户端。
     *
     * @param properties 对象存储配置
     * @return S3 客户端
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.storage.enabled", havingValue = "true")
    S3Client s3Client(ObjectStorageProperties properties) {
        requireConfigured(properties.getEndpoint(), "STORAGE_ENDPOINT");
        requireConfigured(properties.getAccessKey(), "STORAGE_ACCESS_KEY");
        requireConfigured(properties.getSecretKey(), "STORAGE_SECRET_KEY");
        requireConfigured(properties.getBucket(), "STORAGE_BUCKET");
        requireConfigured(properties.getRegion(), "STORAGE_REGION");
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        properties.getAccessKey(), properties.getSecretKey())))
                .forcePathStyle(properties.isPathStyle())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }

    /** 校验启用对象存储时必需的配置。 */
    private void requireConfigured(String value, String environmentName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("启用对象存储时必须配置 " + environmentName);
        }
    }
}
