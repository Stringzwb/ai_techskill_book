package com.aitechskill.book.document.domain.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 已发布文档列表项。
 *
 * @param id 文档主键
 * @param title 文档标题
 * @param summary 文档摘要
 * @param readingMinutes 预计阅读分钟数
 * @param publishedAt 发布时间
 * @param tags 关联标签
 */
public record DocumentSummaryResponse(
        long id,
        String title,
        String summary,
        int readingMinutes,
        LocalDateTime publishedAt,
        List<DocumentTagResponse> tags) {
}
