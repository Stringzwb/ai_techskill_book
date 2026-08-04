package com.aitechskill.book.user.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.storage.domain.StorageObjectRequest;
import com.aitechskill.book.storage.domain.StoredObject;
import com.aitechskill.book.storage.domain.StoredObjectContent;
import com.aitechskill.book.storage.service.ObjectStorageService;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

/**
 * 用户头像校验和对象存储实现。
 */
@Service
public class DefaultAvatarStorageService implements AvatarStorageService {

    private final ObjectStorageService objectStorageService;
    private final long maxSize;

    public DefaultAvatarStorageService(
            ObjectStorageService objectStorageService,
            @Value("${app.user.avatar-max-size}") DataSize maxSize) {
        this.objectStorageService = objectStorageService;
        this.maxSize = maxSize.toBytes();
    }

    /** 校验头像真实格式并写入标准业务目录。 */
    @Override
    public String save(Long userId, byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw invalid("请选择需要上传的头像");
        }
        if (content.length > maxSize) {
            throw new BusinessException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "AVATAR_TOO_LARGE",
                    "头像不能超过 " + DataSize.ofBytes(maxSize).toMegabytes() + "MB");
        }
        AvatarFileType fileType = AvatarFileType.detect(content);
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (fileType == null || !fileType.matchesContentType(normalizedContentType)) {
            throw invalid("仅支持 JPG、PNG 或 WebP 格式的头像");
        }
        StoredObject storedObject = objectStorageService.put(new StorageObjectRequest(
                "avatar",
                userId.toString(),
                fileType.extension,
                fileType.contentType,
                content));
        return storedObject.objectKey();
    }

    /** 读取头像内容。 */
    @Override
    public StoredObjectContent get(String objectKey) {
        return objectStorageService.get(objectKey);
    }

    /** 删除头像对象。 */
    @Override
    public void delete(String objectKey) {
        objectStorageService.delete(objectKey);
    }

    /** 创建头像校验异常。 */
    private BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_AVATAR", message);
    }

    /** 支持的头像格式及文件头校验。 */
    private enum AvatarFileType {
        JPEG("jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        PNG("png", "image/png", new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        }),
        WEBP("webp", "image/webp", new byte[] {0x52, 0x49, 0x46, 0x46});

        private final String extension;
        private final String contentType;
        private final byte[] signature;

        AvatarFileType(String extension, String contentType, byte[] signature) {
            this.extension = extension;
            this.contentType = contentType;
            this.signature = signature;
        }

        /** 按文件头识别真实图片格式。 */
        private static AvatarFileType detect(byte[] content) {
            for (AvatarFileType type : values()) {
                if (type.matchesSignature(content)) {
                    return type;
                }
            }
            return null;
        }

        /** 校验浏览器声明的媒体类型。 */
        private boolean matchesContentType(String declaredContentType) {
            return contentType.equals(declaredContentType)
                    || (this == JPEG && "image/jpg".equals(declaredContentType));
        }

        /** 校验图片文件头，WebP 还需包含 WEBP 标识。 */
        private boolean matchesSignature(byte[] content) {
            if (content.length < signature.length
                    || !Arrays.equals(signature, Arrays.copyOf(content, signature.length))) {
                return false;
            }
            return this != WEBP || (content.length >= 12
                    && content[8] == 0x57
                    && content[9] == 0x45
                    && content[10] == 0x42
                    && content[11] == 0x50);
        }
    }
}
