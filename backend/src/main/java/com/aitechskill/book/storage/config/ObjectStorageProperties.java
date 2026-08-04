package com.aitechskill.book.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3 兼容对象存储配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class ObjectStorageProperties {

    /** 是否启用对象存储。 */
    private boolean enabled;

    /** S3 API 端点。 */
    private String endpoint;

    /** 访问密钥标识。 */
    private String accessKey;

    /** 访问密钥。 */
    private String secretKey;

    /** 项目存储桶名称。 */
    private String bucket;

    /** S3 签名区域。 */
    private String region = "us-east-1";

    /** 对象键环境前缀。 */
    private String environment = "local";

    /** 是否使用 Path Style 地址。 */
    private boolean pathStyle = true;
}
