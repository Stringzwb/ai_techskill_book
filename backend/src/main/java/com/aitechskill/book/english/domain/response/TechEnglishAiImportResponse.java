package com.aitechskill.book.english.domain.response;

import java.util.List;

/**
 * AI 截图导入结果。
 *
 * @param batchUuid 导入批次标识
 * @param importType 导入模式，固定为 AUTO
 * @param sourceName 语料来源
 * @param imageCount 上传截图数
 * @param createdCount 创建语料数
 * @param items 已创建语料
 */
public record TechEnglishAiImportResponse(
        String batchUuid,
        String importType,
        String sourceName,
        int imageCount,
        int createdCount,
        List<TechEnglishCorpusDetailResponse> items) {
}
