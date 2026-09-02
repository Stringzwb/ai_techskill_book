package com.aitechskill.book.english.domain.response;

import java.time.LocalDateTime;

/**
 * 一次页面上传会话的识图历史摘要。
 */
public record TechEnglishRecognitionHistorySummaryResponse(
        String sessionUuid,
        String status,
        String sourceName,
        String batchName,
        String scenario,
        int chunkCount,
        int completedChunkCount,
        int imageCount,
        int itemCount,
        int importedChunkCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /** 兼容未保存批次名称的旧记录。 */
    public TechEnglishRecognitionHistorySummaryResponse(
            String sessionUuid,
            String status,
            String sourceName,
            String scenario,
            int chunkCount,
            int completedChunkCount,
            int imageCount,
            int itemCount,
            int importedChunkCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this(sessionUuid, status, sourceName, null, scenario, chunkCount, completedChunkCount,
                imageCount, itemCount, importedChunkCount, createdAt, updatedAt);
    }
}
