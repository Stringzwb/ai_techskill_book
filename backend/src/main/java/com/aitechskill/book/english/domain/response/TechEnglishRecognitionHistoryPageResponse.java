package com.aitechskill.book.english.domain.response;

import java.util.List;

/**
 * 技术英语识图历史分页。
 */
public record TechEnglishRecognitionHistoryPageResponse(
        long total,
        int page,
        int size,
        int totalPages,
        List<TechEnglishRecognitionHistorySummaryResponse> items) {
}
