package com.aitechskill.book.english.domain.response;

import java.time.Instant;
import java.util.List;

/**
 * 截图识别草稿，用户选择标签并确认后才正式入库。
 *
 * @param sessionUuid 页面一次上传会话标识
 * @param batchUuid 草稿批次标识
 * @param chunkIndex 当前并发子任务序号
 * @param chunkCount 并发子任务总数
 * @param importType 识别模式，固定为 AUTO
 * @param sourceName 语料来源
 * @param imageCount 截图数量
 * @param itemCount 识别语料数量
 * @param expiresAt 草稿过期时间
 * @param items 待确认识别结果
 */
public record TechEnglishAiRecognitionResponse(
        String sessionUuid,
        String batchUuid,
        int chunkIndex,
        int chunkCount,
        String importType,
        String sourceName,
        String batchName,
        int imageCount,
        int itemCount,
        Instant expiresAt,
        List<TechEnglishAiRecognitionItemResponse> items) {

    /** 兼容未返回批次名称的旧调用方。 */
    public TechEnglishAiRecognitionResponse(
            String sessionUuid,
            String batchUuid,
            int chunkIndex,
            int chunkCount,
            String importType,
            String sourceName,
            int imageCount,
            int itemCount,
            Instant expiresAt,
            List<TechEnglishAiRecognitionItemResponse> items) {
        this(sessionUuid, batchUuid, chunkIndex, chunkCount, importType, sourceName, null,
                imageCount, itemCount, expiresAt, items);
    }
}
