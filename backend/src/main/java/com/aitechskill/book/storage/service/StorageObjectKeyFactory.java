package com.aitechskill.book.storage.service;

import com.aitechskill.book.storage.config.ObjectStorageProperties;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 生成不包含原始文件名的标准对象键。
 */
@Component
public class StorageObjectKeyFactory {

    private static final Pattern SAFE_SEGMENT = Pattern.compile("[a-zA-Z0-9_-]+");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[a-zA-Z0-9]+");
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final ObjectStorageProperties properties;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;

    /**
     * 创建生产使用的对象键生成器。
     *
     * @param properties 对象存储配置
     */
    public StorageObjectKeyFactory(ObjectStorageProperties properties) {
        this(properties, Clock.systemUTC(), UUID::randomUUID);
    }

    /** 测试使用的可控构造方法。 */
    StorageObjectKeyFactory(ObjectStorageProperties properties, Clock clock, Supplier<UUID> uuidSupplier) {
        this.properties = properties;
        this.clock = clock;
        this.uuidSupplier = uuidSupplier;
    }

    /**
     * 生成“环境/业务/年/月/归属ID/UUID.扩展名”对象键。
     *
     * @param business 业务目录
     * @param ownerId 归属标识
     * @param extension 安全扩展名
     * @return 标准对象键
     */
    public String create(String business, String ownerId, String extension) {
        String environment = safeSegment(properties.getEnvironment(), "环境").toLowerCase(Locale.ROOT);
        String safeBusiness = safeSegment(business, "业务目录").toLowerCase(Locale.ROOT);
        String safeOwnerId = safeSegment(ownerId, "归属标识");
        if (extension == null || !SAFE_EXTENSION.matcher(extension).matches()) {
            throw new IllegalArgumentException("文件扩展名不符合存储规范");
        }
        String datePath = DATE_PATH.format(LocalDate.now(clock.withZone(ZoneOffset.UTC)));
        return "%s/%s/%s/%s/%s.%s".formatted(
                environment,
                safeBusiness,
                datePath,
                safeOwnerId,
                uuidSupplier.get().toString().replace("-", ""),
                extension.toLowerCase(Locale.ROOT));
    }

    /** 校验对象键目录片段。 */
    private String safeSegment(String value, String label) {
        if (value == null || !SAFE_SEGMENT.matcher(value).matches()) {
            throw new IllegalArgumentException(label + "不符合存储规范");
        }
        return value;
    }
}
