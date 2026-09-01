package com.aitechskill.book.english.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.english.domain.TechEnglishImageContent;
import com.aitechskill.book.storage.domain.StorageObjectStreamRequest;
import com.aitechskill.book.storage.domain.StoredObject;
import com.aitechskill.book.storage.domain.StoredObjectStream;
import com.aitechskill.book.storage.service.ObjectStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

/**
 * 技术英语图片语料的文件校验和对象存储。
 */
@Service
public class TechEnglishImageStorageService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp");

    private final ObjectStorageService storage;
    private final long imageMaxSize;

    public TechEnglishImageStorageService(
            ObjectStorageService storage,
            @Value("${app.community.image-max-size}") DataSize imageMaxSize) {
        this.storage = storage;
        this.imageMaxSize = imageMaxSize.toBytes();
    }

    /** 校验并保存图片语料文件。 */
    public StoredObject save(long ownerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid("请选择图片文件");
        }
        String extension = extension(file.getOriginalFilename());
        if (!IMAGE_EXTENSIONS.contains(extension)) {
            throw invalid("图片仅支持 JPG、PNG、GIF 或 WebP");
        }
        if (file.getSize() > imageMaxSize) {
            throw new BusinessException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "TECH_ENGLISH_IMAGE_TOO_LARGE",
                    "图片不能超过 " + DataSize.ofBytes(imageMaxSize).toMegabytes() + "MB");
        }
        String trustedContentType = contentType(extension, file.getContentType());
        validateSignature(extension, file);
        try {
            return storage.putStream(new StorageObjectStreamRequest(
                    "tech_english", Long.toString(ownerId), extension, trustedContentType,
                    file.getSize(), file.getInputStream()));
        } catch (IOException exception) {
            throw invalid("图片读取失败");
        }
    }

    /** 打开已保存的图片语料。 */
    public TechEnglishImageContent open(String objectKey) {
        StoredObjectStream object = storage.open(objectKey);
        return new TechEnglishImageContent(object.inputStream(), fallbackContentType(object.contentType()), object.contentLength());
    }

    /** 删除上传失败后不再使用的截图对象。 */
    public void delete(String objectKey) {
        if (StringUtils.hasText(objectKey)) {
            storage.delete(objectKey);
        }
    }

    /** 从原始文件名解析安全扩展名。 */
    private String extension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw invalid("图片文件名缺少扩展名");
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!extension.matches("[a-z0-9]+")) {
            throw invalid("图片文件名不合法");
        }
        return extension;
    }

    /** 根据扩展名和浏览器声明的类型生成可信媒体类型。 */
    private String contentType(String extension, String declaredContentType) {
        String expected = CONTENT_TYPES.get(extension);
        String declared = declaredContentType == null ? "" : declaredContentType.toLowerCase(Locale.ROOT);
        if (expected == null || (!expected.equals(declared) && !("jpg".equals(extension) && "image/jpg".equals(declared)))) {
            throw invalid("图片类型与文件扩展名不一致");
        }
        return expected;
    }

    /** 返回安全的图片媒体类型。 */
    private String fallbackContentType(String value) {
        return CONTENT_TYPES.containsValue(value) ? value : "application/octet-stream";
    }

    /** 校验文件头，避免仅修改扩展名和 MIME 冒充图片。 */
    private void validateSignature(String extension, MultipartFile file) {
        byte[] header;
        try (InputStream input = file.getInputStream()) {
            header = input.readNBytes(12);
        } catch (IOException exception) {
            throw invalid("图片读取失败");
        }
        boolean valid = switch (extension) {
            case "jpg", "jpeg" -> hasBytesAt(header, 0, 0xFF, 0xD8, 0xFF);
            case "png" -> hasBytesAt(header, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" -> hasBytesAt(header, 0, 0x47, 0x49, 0x46, 0x38);
            case "webp" -> hasBytesAt(header, 0, 0x52, 0x49, 0x46, 0x46)
                    && hasBytesAt(header, 8, 0x57, 0x45, 0x42, 0x50);
            default -> false;
        };
        if (!valid) {
            throw invalid("图片文件特征与扩展名不匹配");
        }
    }

    /** 校验指定位置的文件头字节。 */
    private boolean hasBytesAt(byte[] bytes, int offset, int... expected) {
        if (bytes.length < offset + expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index += 1) {
            if ((bytes[offset + index] & 0xFF) != expected[index]) {
                return false;
            }
        }
        return true;
    }

    /** 创建图片语料校验异常。 */
    private BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_IMAGE_INVALID", message);
    }
}
