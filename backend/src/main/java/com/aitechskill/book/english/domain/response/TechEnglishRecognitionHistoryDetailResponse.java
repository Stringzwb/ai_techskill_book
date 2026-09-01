package com.aitechskill.book.english.domain.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一次页面上传会话的完整识图记录。
 */
public record TechEnglishRecognitionHistoryDetailResponse(
        String sessionUuid,
        String status,
        String sourceName,
        String scenario,
        int imageCount,
        int itemCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TechEnglishRecognitionHistoryTaskResponse> tasks) {
}
