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
        try {
            return storage.putStream(new StorageObjectStreamRequest(
                    "tech_english", Long.toString(ownerId), extension, contentType(extension, file.getContentType()),
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

    /** 创建图片语料校验异常。 */
    private BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_IMAGE_INVALID", message);
    }
}
