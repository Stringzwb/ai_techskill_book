package com.aitechskill.book.english.domain.response;

import java.time.Instant;
import java.util.List;

/**
 * 截图识别草稿，用户选择标签并确认后才正式入库。
 *
 * @param batchUuid 草稿批次标识
 * @param importType 识别模式，固定为 AUTO
 * @param sourceName 语料来源
 * @param imageCount 截图数量
 * @param itemCount 识别语料数量
 * @param expiresAt 草稿过期时间
 * @param items 待确认识别结果
 */
public record TechEnglishAiRecognitionResponse(
        String batchUuid,
        String importType,
        String sourceName,
        int imageCount,
        int itemCount,
        Instant expiresAt,
        List<TechEnglishAiRecognitionItemResponse> items) {
}
