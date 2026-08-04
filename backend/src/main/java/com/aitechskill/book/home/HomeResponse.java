package com.aitechskill.book.home;

import com.aitechskill.book.article.ArticleSummary;
import java.util.List;

/**
 * 公开首页响应。
 */
public record HomeResponse(
        String productName,
        String headline,
        long articleCount,
        int learningPathCount,
        int categoryCount,
        List<CategorySummary> categories,
        List<ArticleSummary> featuredArticles) {
}
