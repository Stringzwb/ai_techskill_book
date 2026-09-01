package com.aitechskill.book.ai.service;

import com.aitechskill.book.ai.config.AiModelClients;
import com.aitechskill.book.ai.config.AiModelProperties;
import com.aitechskill.book.ai.domain.response.AiChatResponse;
import com.aitechskill.book.common.exception.BusinessException;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通过 LangChain4j 执行文字和图片对话，失败时按顺序切换备用 Key。
 */
@Service
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class AiChatService {

    private static final int MAX_MESSAGE_LENGTH = 20000;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final AiModelClients clients;
    private final AiModelProperties properties;

    public AiChatService(AiModelClients clients, AiModelProperties properties) {
        this.clients = clients;
        this.properties = properties;
    }

    /**
     * 发送单轮文字消息。
     *
     * @param message 用户消息
     * @return 模型回答
     */
    public AiChatResponse chat(String message) {
        String normalizedMessage = normalizeMessage(message);
        return generate(UserMessage.from(normalizedMessage));
    }

    /**
     * 向视觉模型同时发送文字和图片。
     *
     * @param message 用户消息
     * @param image 上传图片
     * @return 模型回答
     */
    public AiChatResponse vision(String message, MultipartFile image) {
        return vision(message, List.of(image));
    }

    /**
     * 向视觉模型同时发送文字和多张图片。
     *
     * @param message 用户消息
     * @param images 上传图片列表
     * @return 模型回答
     */
    public AiChatResponse vision(String message, List<MultipartFile> images) {
        String normalizedMessage = normalizeMessage(message);
        if (images == null || images.isEmpty()) {
            throw invalidImage("请上传图片文件");
        }
        List<Content> contents = new ArrayList<>();
        contents.add(TextContent.from(normalizedMessage));
        for (MultipartFile image : images) {
            ValidatedImage validatedImage = validateImage(image);
            contents.add(ImageContent.from(validatedImage.base64Data(), validatedImage.contentType()));
        }
        UserMessage userMessage = UserMessage.from(contents);
        return generate(userMessage);
    }

    /** 按配置顺序调用模型，当前 Key 失败时自动切换下一个。 */
    private AiChatResponse generate(UserMessage userMessage) {
        RuntimeException lastFailure = null;
        List<ChatMessage> messages = List.of(userMessage);
        for (ChatLanguageModel model : clients.models()) {
            try {
                Response<dev.langchain4j.data.message.AiMessage> response = model.generate(messages);
                if (response == null || response.content() == null || !StringUtils.hasText(response.content().text())) {
                    throw new IllegalStateException("模型未返回文字内容");
                }
                return toResponse(response);
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }
        throw new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI_MODEL_UNAVAILABLE",
                "AI 服务暂不可用，请稍后重试",
                lastFailure);
    }

    /** 转换 LangChain4j 响应且不暴露主备 Key 使用情况。 */
    private AiChatResponse toResponse(Response<dev.langchain4j.data.message.AiMessage> response) {
        TokenUsage usage = response.tokenUsage();
        return new AiChatResponse(
                response.content().text(),
                properties.getModel(),
                usage == null ? null : usage.inputTokenCount(),
                usage == null ? null : usage.outputTokenCount(),
                usage == null ? null : usage.totalTokenCount());
    }

    /** 校验并规范用户消息。 */
    private String normalizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "AI_MESSAGE_REQUIRED", "对话内容不能为空");
        }
        String normalized = message.trim();
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "AI_MESSAGE_TOO_LONG", "对话内容不能超过 20000 字");
        }
        return normalized;
    }

    /** 校验图片体积、MIME 和文件特征后转换 Base64。 */
    private ValidatedImage validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw invalidImage("请上传图片文件");
        }
        long maxSize = properties.getImageMaxSize().toBytes();
        if (image.getSize() > maxSize) {
            throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "AI_IMAGE_TOO_LARGE", "图片体积超过允许上限");
        }
        String contentType = normalizeContentType(image.getContentType());
        if (!SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            throw invalidImage("仅支持 JPEG、PNG、GIF 和 WebP 图片");
        }
        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException exception) {
            throw invalidImage("无法读取上传图片", exception);
        }
        if (bytes.length == 0 || bytes.length > maxSize || !matchesSignature(contentType, bytes)) {
            throw invalidImage("图片文件特征与声明类型不匹配");
        }
        return new ValidatedImage(java.util.Base64.getEncoder().encodeToString(bytes), contentType);
    }

    /** 归一化客户端声明的图片 MIME。 */
    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "";
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return "image/jpg".equals(normalized) ? "image/jpeg" : normalized;
    }

    /** 校验常用图片格式的文件头特征。 */
    private boolean matchesSignature(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
            case "image/png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/gif" -> startsWith(bytes, 0x47, 0x49, 0x46, 0x38);
            case "image/webp" -> startsWith(bytes, 0x52, 0x49, 0x46, 0x46)
                    && hasBytesAt(bytes, 8, 0x57, 0x45, 0x42, 0x50);
            default -> false;
        };
    }

    /** 校验文件是否以指定字节开头。 */
    private boolean startsWith(byte[] bytes, int... expected) {
        return hasBytesAt(bytes, 0, expected);
    }

    /** 校验文件指定位置的字节特征。 */
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

    /** 创建不暴露文件内容的图片校验异常。 */
    private BusinessException invalidImage(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "AI_IMAGE_INVALID", message);
    }

    /** 创建带内部原因的图片校验异常。 */
    private BusinessException invalidImage(String message, Throwable cause) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "AI_IMAGE_INVALID", message, cause);
    }

    /** 已校验的模型图片输入。 */
    private record ValidatedImage(String base64Data, String contentType) {
    }
}
