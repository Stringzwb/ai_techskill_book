package com.aitechskill.book.storage.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.aitechskill.book.storage.domain.StorageObjectRequest;
import com.aitechskill.book.storage.domain.StoredObject;
import com.aitechskill.book.storage.domain.StoredObjectContent;
import com.aitechskill.book.storage.service.S3ObjectStorageService;
import com.aitechskill.book.storage.service.StorageObjectKeyFactory;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 使用显式环境变量验证真实 S3 兼容存储的写入、读取和删除闭环。
 */
class ObjectStorageIntegrationTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "STORAGE_INTEGRATION_TEST", matches = "true")
    void writesReadsAndDeletesSmokeObject() {
        ObjectStorageProperties properties = propertiesFromEnvironment();
        byte[] content = "object-storage-smoke-test".getBytes(StandardCharsets.UTF_8);
        StoredObject storedObject = null;

        try (S3Client s3Client = new ObjectStorageConfig().s3Client(properties)) {
            S3ObjectStorageService storageService = new S3ObjectStorageService(
                    s3Client, properties, new StorageObjectKeyFactory(properties));
            try {
                storedObject = storageService.put(new StorageObjectRequest(
                        "system", "smoke", "txt", "text/plain", content));
                StoredObjectContent downloaded = storageService.get(storedObject.objectKey());

                assertThat(storedObject.objectKey()).startsWith("prod/system/");
                assertThat(downloaded.content()).isEqualTo(content);
                assertThat(downloaded.contentType()).startsWith("text/plain");
            } finally {
                if (storedObject != null) {
                    storageService.delete(storedObject.objectKey());
                }
            }
        }
    }

    /** 从环境变量构造真实对象存储配置，不提供任何仓库内默认凭据。 */
    private ObjectStorageProperties propertiesFromEnvironment() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setEnabled(true);
        properties.setEndpoint(System.getenv("STORAGE_ENDPOINT"));
        properties.setAccessKey(System.getenv("STORAGE_ACCESS_KEY"));
        properties.setSecretKey(System.getenv("STORAGE_SECRET_KEY"));
        properties.setBucket(System.getenv("STORAGE_BUCKET"));
        properties.setRegion(System.getenv().getOrDefault("STORAGE_REGION", "us-east-1"));
        properties.setEnvironment("prod");
        properties.setPathStyle(true);
        return properties;
    }
}
