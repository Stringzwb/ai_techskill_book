package com.aitechskill.book.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aitechskill.book.storage.config.ObjectStorageProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 对象存储键规范测试。
 */
class StorageObjectKeyFactoryTest {

    @Test
    void createsEnvironmentAndBusinessScopedObjectKey() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setEnvironment("prod");
        Clock clock = Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneOffset.UTC);
        UUID uuid = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
        StorageObjectKeyFactory factory = new StorageObjectKeyFactory(properties, clock, () -> uuid);

        String objectKey = factory.create("avatar", "42", "png");

        assertThat(objectKey).isEqualTo("prod/avatar/2026/08/42/123456781234123412341234567890ab.png");
    }

    @Test
    void rejectsUnsafePathSegment() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        StorageObjectKeyFactory factory = new StorageObjectKeyFactory(
                properties, Clock.systemUTC(), UUID::randomUUID);

        assertThatThrownBy(() -> factory.create("../avatar", "42", "png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("业务目录不符合存储规范");
    }
}
