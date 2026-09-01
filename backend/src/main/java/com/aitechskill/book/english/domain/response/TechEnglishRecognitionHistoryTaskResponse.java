package com.aitechskill.book.english.domain.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 识图历史中单个并发子任务的结果。
 */
public record TechEnglishRecognitionHistoryTaskResponse(
        String batchUuid,
        String status,
        int chunkIndex,
        int chunkCount,
        int imageCount,
        int itemCount,
        String errorCode,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        LocalDateTime importedAt,
        List<TechEnglishAiRecognitionItemResponse> items) {
}
