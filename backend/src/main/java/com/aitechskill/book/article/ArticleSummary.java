package com.aitechskill.book.article;

import java.util.Arrays;
import java.util.List;

/**
 * 首页知识专题摘要。
 */
public record ArticleSummary(
        Long id,
        String title,
        String summary,
        String category,
        String difficulty,
        int readMinutes,
        List<String> tags) {

    /**
     * 将专题实体转换为摘要。
     *
     * @param article 专题实体
     * @return 专题摘要
     */
    public static ArticleSummary from(KnowledgeArticle article) {
        List<String> tagList = Arrays.stream(article.getTags().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        return new ArticleSummary(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getCategory(),
                article.getDifficulty(),
                article.getReadMinutes(),
                tagList);
    }
}
